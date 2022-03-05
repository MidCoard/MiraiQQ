package top.focess.qq.core.net;

import com.google.common.collect.Lists;
import top.focess.qq.FocessQQ;
import top.focess.qq.api.exceptions.IllegalPortException;
import top.focess.qq.api.net.PacketPreCodec;
import top.focess.qq.api.net.Receiver;
import top.focess.qq.api.net.ServerReceiver;
import top.focess.qq.api.net.packet.ConnectPacket;
import top.focess.qq.api.net.packet.Packet;
import top.focess.qq.api.net.packet.SidedConnectPacket;
import top.focess.qq.api.util.Pair;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class FocessUDPSocket extends ASocket {

    private final DatagramSocket socket;
    private final DatagramPacket packet;
    private final Thread thread;

    public FocessUDPSocket(int port) throws IllegalPortException {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new IllegalPortException(port);
        }
        this.packet = new DatagramPacket(new byte[1024*1024],1024*1024);
        this.thread = new Thread(()->{
            FocessQQ.getLogger().debugLang("start-focess-udp-socket",port);
            while (!socket.isClosed()) {
                try {
                    socket.receive(this.packet);
                    PacketPreCodec packetPreCodec = new PacketPreCodec();
                    packetPreCodec.push(this.packet.getData(),this.packet.getOffset(),this.packet.getLength());
                    Packet packet = packetPreCodec.readPacket();
                    if (packet != null) {
                        if (packet instanceof SidedConnectPacket) {
                            String name = ((SidedConnectPacket) packet).getName();
                            packet = new ConnectPacket(this.packet.getAddress().getHostName(),this.packet.getPort(),name);
                        }
                        for (Pair<Receiver, Method> pair : packetMethods.getOrDefault(packet.getClass(), Lists.newArrayList())) {
                            Method method = pair.getValue();
                            try {
                                method.setAccessible(true);
                                Object o = method.invoke(pair.getKey(), packet);
                                if (o != null) {
                                    PacketPreCodec handler = new PacketPreCodec();
                                    handler.writePacket((Packet)o);
                                    DatagramPacket sendPacket = new DatagramPacket(handler.getBytes(),handler.getBytes().length,this.packet.getSocketAddress());
                                    socket.send(sendPacket);
                                }
                            } catch (Exception e) {
                                FocessQQ.getLogger().thrLang("exception-handle-packet", e);
                            }
                        }
                    }
                } catch (IOException e) {
                    FocessQQ.getLogger().thrLang("exception-focess-udp-socket",e);
                }
            }
        });
        this.thread.start();
    }

    @Override
    public void registerReceiver(Receiver receiver) {
        if (!(receiver instanceof ServerReceiver))
            throw new UnsupportedOperationException();
        super.registerReceiver(receiver);
    }

    @Override
    public boolean containsServerSide() {
        return true;
    }

    @Override
    public boolean containsClientSide() {
        return false;
    }

    @Override
    public boolean close() {
        boolean ret = false;
        for (Receiver receiver: receivers)
            ret = ret || receiver.close();
        this.socket.close();
        return ret;
    }

    public void sendPacket(String host, int port, Packet packet) {
        PacketPreCodec handler = new PacketPreCodec();
        handler.writePacket(packet);
        DatagramPacket sendPacket = new DatagramPacket(handler.getBytes(),handler.getBytes().length,new InetSocketAddress(host,port));
        try {
            this.socket.send(sendPacket);
        } catch (IOException e) {
            FocessQQ.getLogger().thrLang("exception-send-packet",e);
        }
    }
}

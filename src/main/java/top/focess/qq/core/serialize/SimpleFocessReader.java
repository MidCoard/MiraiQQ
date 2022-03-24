package top.focess.qq.core.serialize;

import com.google.common.collect.Maps;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import org.checkerframework.checker.nullness.qual.Nullable;
import top.focess.qq.api.serialize.FocessReader;
import top.focess.qq.api.serialize.SerializationParseException;
import top.focess.qq.core.plugin.PluginCoreClassLoader;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static top.focess.qq.core.serialize.Opcodes.*;

public class SimpleFocessReader extends FocessReader {

    private static final PureJavaReflectionProvider PROVIDER = new PureJavaReflectionProvider();

    private static final Map<Class<?>, Reader<?>> CLASS_READER_MAP = Maps.newHashMap();

    static {
        CLASS_READER_MAP.put(ArrayList.class, (Reader<ArrayList>) (t, reader) -> {
            ArrayList list = new ArrayList();
            int length = reader.readInt();
            for (int i = 0;i<length;i++)
                list.add(reader.readObject());
            return list;
        });

        CLASS_READER_MAP.put(LinkedList.class, (Reader<LinkedList>) (t, reader) -> {
            LinkedList list = new LinkedList();
            int length = reader.readInt();
            for (int i = 0;i<length;i++)
                list.offer(reader.readObject());
            return list;
        });

        CLASS_READER_MAP.put(HashMap.class, (Reader<HashMap>) (t, reader) -> {
            HashMap hashMap = new HashMap();
            int length = reader.readInt();
            for (int i = 0;i<length;i++)
                hashMap.put(reader.readObject(),reader.readObject());
            return hashMap;
        });

        CLASS_READER_MAP.put(TreeMap.class, (Reader<TreeMap>) (t, reader) -> {
            TreeMap treeMap = new TreeMap();
            int length = reader.readInt();
            for (int i = 0;i<length;i++)
                treeMap.put(reader.readObject(),reader.readObject());
            return treeMap;
        });

        CLASS_READER_MAP.put(HashSet.class, (Reader<HashSet>) (t, reader) -> {
            HashSet hashSet = new HashSet();
            int length = reader.readInt();
            for (int i = 0;i<length;i++)
                hashSet.add(reader.readObject());
            return hashSet;
        });

        CLASS_READER_MAP.put(TreeSet.class, (Reader<TreeSet>) (t, reader) -> {
            TreeSet treeSet = new TreeSet();
            int length = reader.readInt();
            for (int i = 0;i<length;i++)
                treeSet.add(reader.readObject());
            return treeSet;
        });

        CLASS_READER_MAP.put(Class.class, (Reader<Class>) (t, reader) -> {
            try {
                String cls = reader.readString();
                switch (cls) {
                    case "byte":
                        return byte.class;
                    case "short":
                        return short.class;
                    case "int":
                        return int.class;
                    case "long":
                        return long.class;
                    case "float":
                        return float.class;
                    case "double":
                        return double.class;
                    case "boolean":
                        return boolean.class;
                    case "char":
                        return char.class;
                    case "void":
                        return void.class;
                    default:
                        return PluginCoreClassLoader.forName(cls);
                }
            } catch (ClassNotFoundException e) {
                throw new SerializationParseException(e);
            }
        });

        CLASS_READER_MAP.put(ConcurrentHashMap.KeySetView.class,(Reader<ConcurrentHashMap.KeySetView>) (t, reader)->{
            ConcurrentHashMap.KeySetView keySetView = ConcurrentHashMap.newKeySet();
            int length = reader.readInt();
            for (int i = 0;i<length;i++)
                keySetView.add(reader.readObject());
            return keySetView;
        });
    }

    private final byte[] bytes;

    private int pointer;

    public SimpleFocessReader(byte[] bytes) {
        this.bytes = bytes;
        this.pointer = 0;
    }

    private int readInt() {
        int r = 0;
        for (int i = 0; i < 4; i++)
            r += Byte.toUnsignedInt(bytes[pointer++]) << (i * 8);
        return r;
    }

    private long readLong() {
        long r = 0L;
        for (int i = 0; i < 8; i++)
            r += Byte.toUnsignedLong(bytes[pointer++]) << (i * 8L);
        return r;
    }

    private String readString() {
        int length = readInt();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++)
            bytes[i] = this.bytes[pointer++];
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    private double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    private byte readByte() {
        return this.bytes[pointer++];
    }

    private short readShort() {
        short r = 0;
        for (int i = 0; i < 2; i++)
            r += (short) Byte.toUnsignedInt(bytes[pointer++]) << (i * 8);
        return r;
    }

    private char readChar(){
        return (char) readShort();
    }

    private boolean readBoolean(){
        return readByte() == 1;
    }

    @Nullable
    public Object read() {
        if (this.pointer >= this.bytes.length)
            throw new SerializationParseException("Read over");
        byte start = readByte();
        if (start != C_START)
            throw new SerializationParseException("Start code is not correct");
        Object o = readObject();
        byte end = readByte();
        if (end != C_END)
            throw new SerializationParseException("End code is not correct");
        return o;
    }

    private Class<?> readClass() {
        String cls = readString();
        switch (cls) {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
            default:
                try {
                    return PluginCoreClassLoader.forName(cls);
                } catch (ClassNotFoundException e) {
                    throw new SerializationParseException(e);
                }
        }
    }

    @Nullable
    private <T> Object readObject() {
        byte type = readByte();
        switch (type) {
            case C_NULL:
                return null;
            case C_BYTE:
                return readByte();
            case C_SHORT:
                return readShort();
            case C_INT:
                return readInt();
            case C_LONG:
                return readLong();
            case C_FLOAT:
                return readFloat();
            case C_DOUBLE:
                return readDouble();
            case C_BOOLEAN:
                return readBoolean();
            case C_CHAR:
                return readChar();
            case C_STRING:
                return readString();
            case C_ARRAY: {
                Class<?> cls = readClass();
                int length = readInt();
                Object array = Array.newInstance(cls, length);
                for (int i = 0; i < length; i++)
                    Array.set(array, i, readObject());
                return array;
            }
            case C_SERIALIZABLE: {
                String className = readString();
                Object o = readObject();
                try {
                    Class<?> cls = PluginCoreClassLoader.forName(className);
                    Method method = cls.getMethod("deserialize", Map.class);
                    return method.invoke(null, o);
                } catch (Exception e) {
                    throw new SerializationParseException(e);
                }
            }
            case C_OBJECT: {
                String className = readString();
                int length = readInt();
                try {
                    Class<?> cls = PluginCoreClassLoader.forName(className);
                    Object o = PROVIDER.newInstance(cls);
                    for (int i = 0; i < length; i++) {
                        byte field = readByte();
                        if (field != C_FIELD)
                            throw new SerializationParseException("Field code is not correct");
                        String fieldName = readString();
                        Field f = cls.getDeclaredField(fieldName);
                        f.setAccessible(true);
                        f.set(o, readObject());
                    }
                    return o;
                } catch (Exception e) {
                    throw new SerializationParseException(e);
                }
            }
            case C_RESERVED: {
                String className = readString();
                try {
                    Class<T> cls = (Class<T>) PluginCoreClassLoader.forName(className);
                    Reader<T> reader;
                    if ((reader = (Reader<T>) CLASS_READER_MAP.get(cls)) != null)
                        return reader.read(cls, this);
                } catch (ClassNotFoundException e) {
                    throw new SerializationParseException(e);
                }
            }
            default:
                throw new SerializationParseException("Unknown type code");
        }
    }

    private interface Reader<T> {
        T read(Class<T> cls, SimpleFocessReader reader) throws SerializationParseException;
    }

}

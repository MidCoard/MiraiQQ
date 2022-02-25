package com.focess.api.command;

import com.focess.api.command.data.*;
import com.focess.api.command.data.StringBuffer;
import com.focess.api.plugin.Plugin;
import com.focess.api.util.Pair;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Map;

/**
 * Store and parser arguments for better CommandExecutor usage.
 */
public class DataCollection {

    private static final Map<Plugin, List<Pair<Class<?>, BufferGetter>>> PLUGIN_BUFFER_MAP = Maps.newConcurrentMap();
    private final IntBuffer intBuffer;
    private final DoubleBuffer doubleBuffer;
    private final BooleanBuffer booleanBuffer;
    private final LongBuffer longBuffer;
    private final StringBuffer defaultBuffer;
    private final PluginBuffer pluginBuffer;
    private final CommandBuffer commandBuffer;
    private final Map<Class<?>, DataBuffer> buffers = Maps.newHashMap();

    /**
     * Initialize the DataCollection with fixed size.
     *
     * @param size the arguments size
     */
    public DataCollection(int size) {
        //todo fix memory unused!
        this.defaultBuffer = StringBuffer.allocate(size);
        this.intBuffer = IntBuffer.allocate(size);
        this.doubleBuffer = DoubleBuffer.allocate(size);
        this.booleanBuffer = BooleanBuffer.allocate(size);
        this.longBuffer = LongBuffer.allocate(size);
        this.pluginBuffer = PluginBuffer.allocate(size);
        this.commandBuffer = CommandBuffer.allocate(size);
        for (Plugin plugin : PLUGIN_BUFFER_MAP.keySet())
            for (Pair<Class<?>,BufferGetter> pair : PLUGIN_BUFFER_MAP.get(plugin))
                buffers.put(pair.getKey(), pair.getValue().newBuffer(size));
    }

    /**
     * Register the getter of the buffer by plugin
     *
     * @param plugin the plugin
     * @param c the class type of the buffer's elements.
     * @param bufferGetter the getter of the buffer
     */
    public static void register(Plugin plugin, Class<?> c, BufferGetter bufferGetter) {
        PLUGIN_BUFFER_MAP.compute(plugin, (k, v) -> {
            if (v == null)
                v = Lists.newArrayList();
            v.add(Pair.of(c, bufferGetter));
            return v;
        });
    }

    /**
     * Unregister the getter of the buffers by plugin
     * @param plugin the plugin
     */
    public static void unregister(Plugin plugin) {
        PLUGIN_BUFFER_MAP.remove(plugin);
    }

    /**
     * Unregister all the getter of the buffers
     */
    public static boolean unregisterAll() {
        boolean ret = !PLUGIN_BUFFER_MAP.isEmpty();
        PLUGIN_BUFFER_MAP.clear();
        return ret;
    }


    /**
     * Flip all the buffers. Make them all readable.
     */
    void flip() {
        this.defaultBuffer.flip();
        this.intBuffer.flip();
        this.doubleBuffer.flip();
        this.booleanBuffer.flip();
        this.longBuffer.flip();
        this.pluginBuffer.flip();
        this.commandBuffer.flip();
        for (Class<?> c : buffers.keySet())
            buffers.get(c).flip();
    }

    /**
     * Write a String argument
     *
     * @param s String argument
     */
    void write(String s) {
        defaultBuffer.put(s);
    }

    /**
     * Write a int argument
     *
     * @param i int argument
     */
    void writeInt(int i) {
        intBuffer.put(i);
    }

    /**
     * Write a double argument
     *
     * @param d double argument
     */
    void writeDouble(double d) {
        doubleBuffer.put(d);
    }

    /**
     * Write a boolean argument
     *
     * @param b boolean argument
     */
    void writeBoolean(boolean b) {
        booleanBuffer.put(b);
    }

    /**
     * Write a long argument
     *
     * @param l long argument
     */
    void writeLong(long l) {
        longBuffer.put(l);
    }

    /**
     * Get String argument in order
     *
     * @return the String argument in order
     */
    public String get() {
        return defaultBuffer.get();
    }

    /**
     * Get int argument in order
     *
     * @return the int argument in order
     */
    public int getInt() {
        return intBuffer.get();
    }

    /**
     * Get double argument in order
     *
     * @return the double argument in order
     */
    public double getDouble() {
        return doubleBuffer.get();
    }

    /**
     * Get boolean argument in order
     *
     * @return the boolean argument in order
     */
    public boolean getBoolean() {
        return booleanBuffer.get();
    }

    /**
     * Get long argument in order
     *
     * @return the long argument in order
     */
    public long getLong() {
        return longBuffer.get();
    }

    /**
     * Write a Plugin argument
     *
     * @param p Plugin argument
     */
    public void writePlugin(Plugin p) {
        this.pluginBuffer.put(p);
    }

    /**
     * Get Plugin argument in order
     *
     * @return the Plugin argument in order
     */
    public Plugin getPlugin() {
        return this.pluginBuffer.get();
    }

    /**
     * Get Command argument in order
     * @return the Command argument in order
     */
    public Command getCommand() {
        return this.commandBuffer.get();
    }

    /**
     * Write a Command argument
     * @param command Command argument
     */
    public void writeCommand(Command command) {
        this.commandBuffer.put(command);
    }

    /**
     * Write customize buffer element
     *
     * @param cls the buffer elements' class
     * @param t the buffer element
     * @param <T> the buffer elements' type
     * @throws UnsupportedOperationException if the buffer is not registered
     */
    public <T> void write(Class<T> cls, T t) {
        buffers.compute(cls, (Key, value) -> {
            if (value == null)
                throw new UnsupportedOperationException();
            value.put(t);
            return value;
        });
    }

    /**
     * Get customize buffer element
     *
     * @param c the buffer elements' class
     * @param <T> the buffer elements' type
     * @throws UnsupportedOperationException if the buffer is not registered
     * @return T the buffer element
     */
    public <T> T get(Class<T> c) {
        if (buffers.get(c) == null)
            throw new UnsupportedOperationException();
        return (T) buffers.get(c).get();
    }

    /**
     * Represents a getter for buffer.
     *
     * This is a functional interface whose functional method is {@link BufferGetter#newBuffer(int)}.
     */
    @FunctionalInterface
    public interface BufferGetter {
        /**
         * Instance a buffer with fixed size
         *
         * @param size the initialized size of the buffer
         * @return the buffer
         */
        DataBuffer<?> newBuffer(int size);
    }
}

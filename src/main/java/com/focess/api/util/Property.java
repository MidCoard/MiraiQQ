package com.focess.api.util;

import com.focess.Main;

/**
 * Store some default properties of this framework.
 */
public class Property {

    /**
     * Indicate whether it has the key or not
     *
     * @param key the MiraiQQ framework properties' key
     * @return true if it has the key, false otherwise
     */
    public static boolean hasKey(String key) {
        return Main.MainPlugin.getProperties().containsKey(key);
    }

    /**
     * Set the property
     *
     * @param key the MiraiQQ framework properties' key
     * @param value the MiraiQQ framework properties' value
     */
    public static void put(String key, Object value) {
        Main.MainPlugin.getProperties().put(key, value);
    }

    /**
     * Get the value of the key in the properties
     *
     * @param key the MiraiQQ framework properties' key
     * @param <T> the desired T type
     * @throws ClassCastException if the desired T type is not equal to its original type
     * @return the desired value
     */
    public static <T> T get(String key) {
        return (T) Main.MainPlugin.getProperties().get(key);
    }

    /**
     * Remove the property
     *
     * @param key the MiraiQQ framework properties' key
     */
    public static void remove(String key) {
        Main.MainPlugin.getProperties().remove(key);
    }
}

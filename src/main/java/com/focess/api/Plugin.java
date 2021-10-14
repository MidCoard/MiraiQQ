package com.focess.api;

import com.focess.Main;
import com.focess.api.annotation.EventHandler;
import com.focess.api.event.Event;
import com.focess.api.event.Listener;
import com.focess.api.event.ListenerHandler;
import com.focess.commands.LoadCommand;
import com.focess.api.util.yaml.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * Represent a Plugin class that can be load, enable and disable. Also, provide plenty of API for the plugin to get better with this framework.
 * You should declare {@link com.focess.api.annotation.PluginType} to this class.
 */
public abstract class Plugin {

    private static final String path = Plugin.class.getProtectionDomain().getCodeSource().getLocation().getFile();

    /**
     * The plugin name
     */
    private final String name;

    /**
     * The plugin configuration stored in YAML
     */
    private final YamlConfiguration configuration;

    /**
     * The plugin configuration file
     */
    private final File config;

    public Plugin(String name) {
        this.name = name;
        if (!getDefaultFolder().exists())
            getDefaultFolder().mkdirs();
        config = new File(getDefaultFolder(), "config.yml");
        if (!config.exists()) {
            try {
                config.createNewFile();
            } catch (IOException e) {
                Main.getLogger().thr("Create Config File Exception",e);
            }
        }
        configuration = YamlConfiguration.loadFile(getConfigFile());
    }

    /**
     * Get Plugin instance by the class instance
     *
     * @see LoadCommand#getPlugin(Class)
     * @param plugin
     * @return
     */
    public static Plugin getPlugin(Class<? extends Plugin> plugin) {
        return LoadCommand.getPlugin(plugin);
    }

    /**
     * Get Plugin instance by the name
     *
     * @see LoadCommand#getPlugin(String)
     * @param name
     * @return
     */
    public static Plugin getPlugin(String name) {
        return LoadCommand.getPlugin(name);
    }

    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Used to initialize the plugin
     */
    public abstract void enable();

    /**
     * Used to save some data of the plugin
     */
    public abstract void disable();

    @NotNull
    public File getDefaultFolder() {
        return new File(new File(new File(path).getParent(), "plugins"), this.getName());
    }

    @NotNull
    public File getConfigFile() {
        return config;
    }

    @NotNull
    public YamlConfiguration getConfig() {
        return configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Plugin plugin = (Plugin) o;

        return Objects.equals(name, plugin.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    /**
     * Register the listener into the Event chain
     *
     * @param listener the listener need to be registered
     */
    public void registerListener(Listener listener) {
        ListenerHandler.addListener(this, listener);
        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventHandler handler;
            if ((handler = method.getAnnotation(EventHandler.class)) != null) {
                if (method.getParameterTypes().length == 1) {
                    Class<?> eventClass = method.getParameterTypes()[0];
                    if (Event.class.isAssignableFrom(eventClass) && !Modifier.isAbstract(eventClass.getModifiers())) {
                        try {
                            Field field = eventClass.getDeclaredField("LISTENER_HANDLER");
                            boolean flag = field.isAccessible();
                            field.setAccessible(true);
                            ListenerHandler listenerHandler = (ListenerHandler) field.get(null);
                            field.setAccessible(flag);
                            listenerHandler.addListener(listener, method, handler);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }
}

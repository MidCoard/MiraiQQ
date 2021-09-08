package com.focess.commands;

import com.focess.api.Plugin;
import com.focess.api.annotation.CommandType;
import com.focess.api.annotation.PluginType;
import com.focess.api.command.Command;
import com.focess.api.command.CommandResult;
import com.focess.api.command.CommandSender;
import com.focess.api.event.ListenerHandler;
import com.focess.api.exception.*;
import com.focess.api.util.IOHandler;
import com.focess.commands.util.AnnotationHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SuppressWarnings("unchecked")
public class LoadCommand extends Command {
    public static final PluginCoreClassLoader DEFAULT_CLASS_LOADER = new PluginCoreClassLoader(LoadCommand.class.getClassLoader());
    private static final List<Plugin> registeredPlugins = Lists.newCopyOnWriteArrayList();
    private static final Map<Plugin, PluginClassLoader> loaders = Maps.newHashMap();

    public LoadCommand() {
        super("load", Lists.newArrayList());
    }

    @Nullable
    public static <T extends Plugin> T getPlugin(Class<? extends T> plugin) {
        for (Plugin p : registeredPlugins)
            if (p.getClass().equals(plugin))
                return (T) p;
        return null;
    }

    @NonNull
    public static List<Plugin> getPlugins() {
        return registeredPlugins;
    }

    public static <T extends Plugin> T loadPlugin(Class<T> cls) {
        try {
            T plugin = cls.newInstance();
            if (getPlugin(cls) != null)
                throw new PluginDuplicateException(cls.getName());
            registeredPlugins.add(plugin);
            plugin.enable();
            return plugin;
        } catch (Exception e) {
            if (e instanceof PluginDuplicateException)
                throw (PluginDuplicateException) e;
            else throw new PluginLoadException(cls);
        }
    }

    public static void loadPlugin(Plugin plugin) {
        if (plugin.getClass().getClassLoader() instanceof PluginClassLoader) {
            if (getPlugin(plugin.getClass()) != null)
                throw new PluginDuplicateException(plugin.getName());
            registeredPlugins.add(plugin);
            plugin.enable();
        } else throw new PluginLoaderException(plugin.getName());
    }

    public static void disablePlugin(Plugin plugin) {
        ListenerHandler.unregisterPlugin(plugin);
        registeredPlugins.remove(plugin);
        Command.unregister(plugin);
        try {
            plugin.disable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (plugin.getClass().getClassLoader() instanceof PluginClassLoader)
            try {
                loaders.remove(plugin).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Nullable
    public static Plugin getPlugin(String name) {
        for (Plugin plugin : registeredPlugins)
            if (plugin.getName().equals(name))
                return plugin;
        return null;
    }

    @Override
    public void init() {
        this.addExecutor(1, (sender, data, ioHandler) -> {
            if (sender.isConsole()) {
                String path = data.get();
                File file = new File(path);
                if (file.exists() && file.getName().endsWith(".jar")) {
                    try {
                        PluginClassLoader classLoader = new PluginClassLoader(file);
                        if (classLoader.load())
                            ioHandler.output("Load " + file.getName());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return CommandResult.ALLOW;
                }
                ioHandler.output("File is not existed.");
                return CommandResult.REFUSE;
            }
            return CommandResult.REFUSE;
        });
    }

    @Override
    public void usage(CommandSender commandSender, IOHandler ioHandler) {
        if (commandSender.isConsole())
            ioHandler.output("Use: load [jar-path]");
    }

    private static class PluginCoreClassLoader extends ClassLoader {

        public PluginCoreClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                for (PluginClassLoader classLoader : loaders.values())
                    try {
                        return classLoader.findClass(name, resolve);
                    } catch (ClassNotFoundException ignored) {
                    }
            }
            throw new ClassNotFoundException();
        }
    }

    public static class PluginClassLoader extends URLClassLoader {

        private static final Map<String, Set<File>> afterPlugins = Maps.newHashMap();

        private static final Set<File> afterPluginFiles = Sets.newConcurrentHashSet();
        private static final Map<Class<? extends Annotation>, AnnotationHandler> handlers = Maps.newHashMap();
        private static final AnnotationHandler PLUGIN_TYPE_HANDLER = (c, annotation, classLoader) -> {
            PluginType pluginType = (PluginType) annotation;
            if (!pluginType.loadAfter().equals("") && getPlugin(pluginType.loadAfter()) == null) {
                afterPlugins.compute(pluginType.loadAfter(), (key, value) -> {
                    if (value == null)
                        return Sets.newHashSet(classLoader.file);
                    else value.add(classLoader.file);
                    return value;
                });
                afterPluginFiles.add(classLoader.file);
                return false;
            } else if (!pluginType.loadAfter().equals(""))
                CommandSender.CONSOLE.getIOHandler().output("Load " + classLoader.file.getName());
            if (Plugin.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) {
                try {
                    Plugin plugin;
                    if (!classLoader.addPlugin(plugin = (Plugin) c.newInstance()))
                        throw new PluginDuplicateException(plugin.getName());
                    return true;
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new PluginLoadException((Class<? extends Plugin>) c);
                }
            } else throw new IllegalPluginClassException();
        };

        static {
            handlers.put(CommandType.class, (c, annotation, classLoader) -> {
                CommandType commandType = (CommandType) annotation;
                if (Command.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers())) {
                    try {
                        Plugin plugin = getPlugin(commandType.plugin());
                        if (plugin == null)
                            throw new IllegalCommandClassException();
                        Command command;
                        if (!Command.register(plugin, command = (Command) c.newInstance()))
                            throw new CommandDuplicateException(command.getName());
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new CommandLoadException((Class<? extends Command>) c);
                    }
                } else throw new IllegalCommandClassException();
                return true;
            });
        }

        private final File file;
        private final List<Plugin> plugins = Lists.newArrayList();
        private final List<Class<?>> loadedClasses = Lists.newArrayList();

        public PluginClassLoader(@NonNull File file) throws MalformedURLException {
            super(new URL[]{file.toURI().toURL()}, DEFAULT_CLASS_LOADER);
            this.file = file;
        }

        private boolean addPlugin(@NonNull Plugin plugin) {
            for (Plugin p : plugins)
                if (p.getName().equals(plugin.getName()))
                    return false;
            for (Plugin p : registeredPlugins)
                if (p.getName().equals(plugin.getName()))
                    return false;
            plugins.add(plugin);
            return true;
        }

        public boolean load() {
            try {
                JarFile jarFile = new JarFile(file);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    String name = jarEntry.getName();
                    if (!name.endsWith(".class"))
                        continue;
                    Class<?> c = this.loadClass(name.replace("/", ".").substring(0, name.length() - 6), true);
                    loadedClasses.add(c);
                    if (!analyseClass0(c))
                        return false;
                }
                for (Plugin plugin : plugins)
                    loadPlugin(plugin);
                for (Class<?> c : loadedClasses)
                    analyseClass(c);
                for (Plugin plugin : plugins) {
                    loaders.put(plugin, this);
                    for (File file : afterPlugins.getOrDefault(plugin.getName(), Sets.newHashSet())) {
                        if (afterPluginFiles.contains(file)) {
                            PluginClassLoader pluginClassLoader = new PluginClassLoader(file);
                            if (pluginClassLoader.load()) {
                                CommandSender.CONSOLE.getIOHandler().output("Load " + file.getName());
                                afterPluginFiles.remove(file);
                            }
                        }
                    }
                    afterPlugins.remove(plugin.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        private boolean analyseClass0(@NonNull Class<?> c) {
            Annotation annotation;
            if ((annotation = c.getAnnotation(PluginType.class)) != null)
                return PLUGIN_TYPE_HANDLER.handle(c, annotation, this);
            return true;
        }

        private void analyseClass(@NonNull Class<?> c) {
            for (Class<? extends Annotation> annotation : handlers.keySet()) {
                Annotation a;
                if ((a = c.getAnnotation(annotation)) != null)
                    handlers.get(annotation).handle(c, a, this);
            }
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }

        public Class<?> findClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = this.findLoadedClass(name);
            if (c == null)
                c = this.findClass(name);
            if (resolve)
                resolveClass(c);
            return c;
        }
    }
}

package top.focess.qq.core.schedule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import top.focess.qq.Main;
import top.focess.qq.api.plugin.Plugin;
import top.focess.qq.api.schedule.Scheduler;

import java.util.List;
import java.util.Map;

public abstract class AScheduler implements Scheduler {

    private final Plugin plugin;

    private static final Map<Plugin, List<Scheduler>> PLUGIN_SCHEDULER_MAP = Maps.newHashMap();

    public AScheduler(Plugin plugin){
        this.plugin = plugin;
        PLUGIN_SCHEDULER_MAP.compute(plugin,(k,v)->{
            if (v == null)
                v = Lists.newArrayList();
            v.add(this);
            return v;
        });
    }

    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    public void close() {
        PLUGIN_SCHEDULER_MAP.get(this.plugin).remove(this);
    }

    /**
     * Close all the schedulers belonging to the plugin
     *
     * @param plugin the plugin
     */
    public static void close(Plugin plugin) {
        for (Scheduler scheduler : PLUGIN_SCHEDULER_MAP.getOrDefault(plugin, Lists.newArrayList()))
            scheduler.close();
        PLUGIN_SCHEDULER_MAP.remove(plugin);
    }

    /**
     * Close all the schedulers
     *
     * @return true if there are some schedulers not belonging to MainPlugin not been closed, false otherwise
     */
    public static boolean closeAll() {
        boolean ret = false;
        for (Plugin plugin : PLUGIN_SCHEDULER_MAP.keySet()) {
            if (plugin != Main.getMainPlugin())
                ret = true;
            close(plugin);
        }
        return ret;
    }
}

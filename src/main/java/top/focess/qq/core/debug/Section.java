package top.focess.qq.core.debug;

import top.focess.qq.FocessQQ;
import top.focess.qq.api.schedule.Scheduler;
import top.focess.qq.api.schedule.Schedulers;
import top.focess.qq.api.schedule.Task;

import java.time.Duration;
import java.util.concurrent.Future;

public class Section {

    private static final Scheduler SCHEDULER = Schedulers.newFocessScheduler(FocessQQ.getMainPlugin(), "Section");

    private final String name;
    private final Task task;

    private Section(final String name, final Task task) {
        this.name = name;
        this.task = task;
    }

    public static Section startSection(final String name, final Future<?> task, final Duration timeout) {
        final Task t = SCHEDULER.run(() -> {
            task.cancel(true);
            FocessQQ.getLogger().debugLang("debug-section-timeout", name);
        }, timeout);
        return new Section(name, t);
    }

    public static Section startSection(final String name, final Task task, final Duration timeout) {
        final Task t = SCHEDULER.run(() -> {
            task.cancel(true);
            FocessQQ.getLogger().debugLang("debug-section-timeout", name);
        }, timeout);
        return new Section(name, t);
    }

    public void stop() {
        this.task.cancel();
    }

    public String getName() {
        return this.name;
    }
}

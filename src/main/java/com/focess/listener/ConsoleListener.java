package com.focess.listener;

import com.focess.Main;
import com.focess.api.annotation.EventHandler;
import com.focess.api.event.ConsoleInputEvent;
import com.focess.api.event.EventPriority;
import com.focess.api.event.Listener;
import com.focess.api.util.IOHandler;
import com.focess.util.Pair;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Queue;

public class ConsoleListener implements Listener {

    public static final Queue<Pair<IOHandler,Long>> QUESTS = Lists.newLinkedList();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsoleInput(ConsoleInputEvent event) {
        if (!QUESTS.isEmpty()) {
            Pair<IOHandler,Long> element = QUESTS.poll();
            while (element != null && System.currentTimeMillis() - element.getValue() > 60 * 10 * 1000) {
                element.getKey().input(null);
                element = QUESTS.poll();
            }
            if (element == null)
                return;
            element.getKey().input(event.getInput());
            return;
        }
        try {
            Main.CommandLine.exec(event.getInput());
        } catch (Exception e) {
            Main.getLogger().thr("Exec Command Exception", e);
        }
    }

    /**
     * Register input String listener. (Used to communicate with CommandSender with ioHandler)
     *
     * @param ioHandler the {@link com.focess.api.command.CommandSender#CONSOLE} CommandSender
     */
    public static void registerInputListener(IOHandler ioHandler) {
        QUESTS.add(Pair.of(ioHandler,System.currentTimeMillis()));
    }


}

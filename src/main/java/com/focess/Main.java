package com.focess;

import com.focess.api.Plugin;
import com.focess.api.command.Command;
import com.focess.api.command.CommandSender;
import com.focess.api.event.*;
import com.focess.api.event.BotReloginEvent;
import com.focess.api.event.chat.FriendChatEvent;
import com.focess.api.event.chat.GroupChatEvent;
import com.focess.api.event.request.FriendRequestEvent;
import com.focess.api.event.request.GroupRequestEvent;
import com.focess.api.exceptions.EventSubmitException;
import com.focess.api.util.IOHandler;
import com.focess.commands.*;
import com.focess.listener.ChatListener;
import com.focess.listener.ConsoleListener;
import com.focess.api.util.CombinedFuture;
import com.focess.util.Pair;
import com.focess.api.util.logger.FocessLogger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import kotlin.coroutines.Continuation;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.network.WrongPasswordException;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.LoginSolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.stream.FileImageOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;

public class Main {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(2);

    /**
     * The Focess Logger
     */
    private static final FocessLogger LOG = new FocessLogger();

    /**
     * The Author QQ number
     */
    private static final long AUTHOR_ID = 2624646185L;
    private static final Scanner SCANNER = new Scanner(System.in);

    /**
     * The Mirai Bot Instance
     */
    private static Bot bot;
    private static MainPlugin MAIN_PLUGIN;

    /**
     * Indicate MainPlugin is running. True after MainPlugin is loaded.
     * @see #isReady
     */
    private static boolean running = false;
    private static Listener<GroupMessageEvent> groupMessageEventListener;
    private static Listener<FriendMessageEvent> friendMessageEventListener;
    private static Listener<MessageRecallEvent.GroupRecall> groupRecallEventListener;
    private static Listener<NewFriendRequestEvent> newFriendRequestEventListener;
    private static Listener<BotInvitedJoinGroupRequestEvent> botInvitedJoinGroupRequestEvent;

    private static final Thread SHUTDOWN_HOOK = new Thread("SavingData") {
        @Override
        public void run() {
            if (running) {
                Main.getLogger().fatal("Main Thread shuts down without saving Data. Try to save data now!");
                Main.getLogger().debug("Save log file.");
                saveLogFile();
                saved = true;
                LoadCommand.disablePlugin(MAIN_PLUGIN);
            }
        }
    };

    /**
     * The Mirai Bot user or we call it QQ number
     */
    private static long user;

    /**
     * The Mirai Bot password
     */
    private static String password;
    private static BotConfiguration configuration;

    /**
     * Indicate Mirai Bot logins. True after Mirai Bot logins.
     * @see #isRunning()
     */
    private static volatile boolean ready = false;
    private static boolean saved = false;

    private static final Thread CONSOLE_THREAD = new Thread(() -> {
        Main.getLogger().debug("Wait for Bot is ready.");
        while (!ready);
        Main.getLogger().debug("Bot is ready.");
        Main.getLogger().debug("Start listening console input.");
        while (ready && SCANNER.hasNextLine()) {
            String input = SCANNER.nextLine();
            try {
                EventManager.submit(new ConsoleInputEvent(input));
            } catch (EventSubmitException e) {
                Main.getLogger().thr("Submit Console Input Exception",e);
            }
        }
    });

    public static boolean isRunning() {
        return running;
    }

    public static boolean isReady() {
        return ready;
    }

    @NotNull
    public static FocessLogger getLogger() {
        return LOG;
    }

    @NotNull
    public static Bot getBot() {
        return bot;
    }

    public static long getUser() {
        return user;
    }

    public static long getAuthorId() {
        return AUTHOR_ID;
    }

    /**
     *
     * get Author as a Friend
     *
     * @return Author as a Friend
     */
    @NotNull
    public static Friend getAuthor() {
        return getBot().getFriendOrFail(getAuthorId());
    }

    private static void requestAccountInformation() {
        try {
            IOHandler.getConsoleIoHandler().output("Please input your QQ user id: ");
            String str = SCANNER.nextLine();
            if (str.equals("stop")) {
                if (LoadCommand.getPlugin(MainPlugin.class) != null)
                    Main.exit();
                else {
                    Main.getLogger().debug("Save log file.");
                    saveLogFile();
                    System.exit(0);
                }
            }
            user = Long.parseLong(str);
            IOHandler.getConsoleIoHandler().output("Please input your QQ password: ");
            password = SCANNER.nextLine();
        } catch (Exception e) {
            requestAccountInformation();
        }
    }

    public static void main(String[] args) {
        IOHandler.getConsoleIoHandler().output("MiraiQQ Bot Start!");
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            Main.getLogger().thr("Uncaught Exception",e);
            Main.getLogger().fatal("Main Thread throws an uncaught exception. Force shutdown!");
            Main.exit();
        });
        Main.getLogger().debug("Setup default UncaughtExceptionHandler.");

        SCHEDULED_EXECUTOR_SERVICE.schedule(()->{
            while (!ConsoleListener.QUESTS.isEmpty() && (System.currentTimeMillis() -  ConsoleListener.QUESTS.peek().getValue())  > 60 * 10 * 1000 )
                ConsoleListener.QUESTS.poll().getKey().input(null);
            for (CommandSender sender : ChatListener.QUESTS.keySet()) {
                Queue<Pair<IOHandler, Pair<Boolean,Long>>> queue = ChatListener.QUESTS.get(sender);
                while (!queue.isEmpty() && (System.currentTimeMillis() - queue.peek().getValue().getValue()) > 60 * 10 * 1000)
                    queue.poll().getKey().input(null);
            }
        },1,TimeUnit.MINUTES);

        CONSOLE_THREAD.start();
        Main.getLogger().debug("Start Console Thread.");
        if (args.length == 2) {
            try {
                user = Long.parseLong(args[0]);
                password = args[1];
                Main.getLogger().debug("Use username and password as given.");
            } catch (Exception ignored) {
                requestAccountInformation();
            }
        } else requestAccountInformation();
        try {
            LoadCommand.enablePlugin(new MainPlugin());
            Main.getLogger().debug("Load MainPlugin.");
        } catch (Exception e) {
            Main.getLogger().thr("Load MainPlugin Exception",e);
        }
    }

    private static void login() {
        bot = BotFactory.INSTANCE.newBot(user, password, configuration);
        try {
            bot.login();
        } catch(Exception e) {
            if (e instanceof WrongPasswordException) {
                IOHandler.getConsoleIoHandler().output("Wrong username or password.");
                requestAccountInformation();
                login();
            } else Main.getLogger().thr("Bot Login Exception",e);
        }
        ready = true;
        groupMessageEventListener = bot.getEventChannel().subscribeAlways(GroupMessageEvent.class, event -> {
            GroupChatEvent e = new GroupChatEvent(event.getSender(), event.getMessage(),event.getSource());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException eventSubmitException) {
                Main.getLogger().thr("Submit Group Message Exception",eventSubmitException);
            }
        });
        friendMessageEventListener = bot.getEventChannel().subscribeAlways(FriendMessageEvent.class, event -> {
            FriendChatEvent e = new FriendChatEvent(event.getFriend(), event.getMessage());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException eventSubmitException) {
                Main.getLogger().thr("Submit Friend Message Exception",eventSubmitException);
            }
        });
        groupRecallEventListener = bot.getEventChannel().subscribeAlways(MessageRecallEvent.GroupRecall.class, event -> {
            GroupRecallEvent e = new GroupRecallEvent(event.getAuthor(),event.getMessageIds(),event.getOperator());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException ex) {
                Main.getLogger().thr("Submit Group Recall Exception",ex);
            }
        });
        newFriendRequestEventListener = bot.getEventChannel().subscribeAlways(NewFriendRequestEvent.class,event ->{
            FriendRequestEvent e = new FriendRequestEvent(event.getFromId(),event.getFromNick(),event.getFromGroup(),event.getMessage());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException ex) {
                Main.getLogger().thr("Submit Friend Request Exception",ex);
            }
            if (e.getAccept() != null)
                if (e.getAccept())
                    event.accept();
                else event.reject(e.isBlackList());
        });
        botInvitedJoinGroupRequestEvent = bot.getEventChannel().subscribeAlways(BotInvitedJoinGroupRequestEvent.class, event->{
            GroupRequestEvent e = new GroupRequestEvent(event.getGroupId(),event.getGroupName(),event.getInvitor());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException ex) {
                Main.getLogger().thr("Submit Group Request Exception",ex);
            }
            if (e.getAccept() != null)
                if (e.getAccept())
                    event.accept();
                else event.ignore();
        });
        Main.getLogger().debug("Register message listeners.");
    }

    /**
     * Relogin Bot using given username and password
     */
    public static void relogin() {
        BotReloginEvent event = new BotReloginEvent();
        try {
            EventManager.submit(event);
        } catch (EventSubmitException e) {
            Main.getLogger().thr("Submit Relogin Exception",e);
        }
        if (event.isCancelled())
            return;
        //todo need to be checked
        bot.close();
        login();
        Main.getLogger().debug("Relogin.");
    }

    /**
     * Exit Bot
     */
    public static void exit() {
        SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
            Main.getLogger().fatal("Main Thread waits for more than 5 sec before shutdown. Force shutdown!");
            Runtime.getRuntime().removeShutdownHook(SHUTDOWN_HOOK);
            System.exit(0);
        }, 5, TimeUnit.SECONDS);
        LoadCommand.disablePlugin(MAIN_PLUGIN);
    }

    private static void saveLogFile() {
        try {
            File latest = new File("logs", "latest.log");
            if (latest.exists()) {
                String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
                File target = new File("logs", name + ".log");
                Files.copy(latest, target);
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(new File("logs", name + ".gz")));
                FileInputStream inputStream = new FileInputStream(target);
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0)
                    gzipOutputStream.write(buf, 0, len);
                inputStream.close();
                gzipOutputStream.finish();
                gzipOutputStream.close();
                target.delete();
            }
        } catch (IOException e) {
            Main.getLogger().thr("Save Log Exception",e);
        }
    }

    /**
     *
     * The MainPlugin Plugin is a core plugin in Bot. It will initialize all default settings and make the Bot login.
     * Never instance it! It will be instanced when bot bootstraps automatically.
     *
     */
    public final static class MainPlugin extends Plugin {

        private static Map<String, Object> properties;

        public MainPlugin() {
            super("MainPlugin");
            if (running) {
                Main.getLogger().fatal("Run more that one MainPlugin. Force shutdown!");
                Main.exit();
            }
            MAIN_PLUGIN = this;
        }

        public static Map<String, Object> getProperties() {
            return properties;
        }

        @Override
        public void enable() {
            Main.getLogger().debug("Enable MainPlugin.");
            running = true;
            this.registerListener(new ConsoleListener());
            this.registerListener(new ChatListener());
            Main.getLogger().debug("Register default listeners.");
            properties = getConfig().getValues();
            if (properties == null)
                properties = Maps.newHashMap();
            Main.getLogger().debug("Load properties.");
            Command.register(this, new LoadCommand());
            Command.register(this, new UnloadCommand());
            Command.register(this, new StopCommand());
            Command.register(this, new ReloginCommand());
            Command.register(this, new FriendCommand());
            Command.register(this, new GroupCommand());
            Main.getLogger().debug("Register default commands.");
            configuration = BotConfiguration.getDefault();
            configuration.setProtocol(BotConfiguration.MiraiProtocol.ANDROID_PAD);
            configuration.fileBasedDeviceInfo();
            configuration.setLoginSolver(new LoginSolver() {
                @Nullable
                @Override
                public Object onSolvePicCaptcha(@NotNull Bot bot, byte[] bytes, @NotNull Continuation<? super String> continuation) {
                    try {
                        FileImageOutputStream outputStream = new FileImageOutputStream(new File("captcha.jpg"));
                        outputStream.write(bytes);
                        outputStream.close();
                    } catch (IOException e) {
                        Main.getLogger().thr("CAPTCHA Picture Load Exception",e);
                    }
                    IOHandler.getConsoleIoHandler().output("Please input CAPTCHA: ");
                    return SCANNER.nextLine();
                }

                @Nullable
                @Override
                public Object onSolveSliderCaptcha(@NotNull Bot bot, @NotNull String s, @NotNull Continuation<? super String> continuation) {
                    IOHandler.getConsoleIoHandler().output(s);
                    SCANNER.nextLine();
                    return null;
                }

                @Nullable
                @Override
                public Object onSolveUnsafeDeviceLoginVerify(@NotNull Bot bot, @NotNull String s, @NotNull Continuation<? super String> continuation) {
                    IOHandler.getConsoleIoHandler().output(s);
                    SCANNER.nextLine();
                    return null;
                }
            });
            Main.getLogger().debug("Setup default Bot configuration.");
            login();
            Main.getLogger().debug("Login.");
            File plugins = new File("plugins");
            if (plugins.exists())
                for (File file : plugins.listFiles(file -> file.getName().endsWith(".jar")))
                    try {
                        Future<Boolean> future = CommandLine.exec("load plugins/" + file.getName());
                        future.get();
                    } catch (Exception e) {
                        Main.getLogger().thr("Load Target Plugin Exception",e);
                    }
            Main.getLogger().debug("Load plugins in 'plugins' folder.");
            Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);
            Main.getLogger().debug("Setup shutdown hook.");
            try {
                EventManager.submit(new ServerStartEvent());
            } catch (EventSubmitException e) {
                Main.getLogger().thr("Submit Server Start Exception", e);
            }
        }

        @Override
        public void disable() {
            Main.getLogger().debug("Disable MainPlugin.");
            for (String key : properties.keySet())
                getConfig().set(key, properties.get(key));
            getConfig().save(getConfigFile());
            Main.getLogger().debug("Save properties.");
            for (Plugin plugin : LoadCommand.getPlugins())
                if (!plugin.equals(this))
                    try {
                        LoadCommand.disablePlugin(plugin);
                    } catch (Exception e) {
                        Main.getLogger().thr("Unload Target Plugin Exception",e);
                    }
            Main.getLogger().debug("Unload all loaded plugins without MainPlugin.");
            if (friendMessageEventListener != null)
                friendMessageEventListener.complete();
            if (groupMessageEventListener != null)
                groupMessageEventListener.complete();
            if (groupRecallEventListener != null)
                groupRecallEventListener.complete();
            if (newFriendRequestEventListener != null)
                newFriendRequestEventListener.complete();
            if (botInvitedJoinGroupRequestEvent != null)
                botInvitedJoinGroupRequestEvent.complete();
            Main.getLogger().debug("Complete all registered listeners.");
            if (!saved) {
                Main.getLogger().debug("Save log file.");
                saveLogFile();
                saved = true;
                ready = false;
                running = false;
            }
            EXECUTOR.shutdownNow();
            SCHEDULED_EXECUTOR_SERVICE.shutdownNow();
            System.exit(0);
        }

    }


    /**
     * The CommandLine Tool Class can be used to exec command with customize executor, arguments and receiver.
     */
    public static class CommandLine {

        /**
         * Execute command using {@link CommandSender#CONSOLE}
         *
         * @param command the command Console executes.
         * @return a Future representing pending completion of the command
         */
        @NotNull
        public static Future<Boolean> exec(String command) {
            return exec(CommandSender.CONSOLE, command);
        }

        /**
         * Execute command with sender
         *
         * @param sender the executor
         * @param command the command CommandSender executes.
         * @return a Future representing pending completion of the command
         */
        @NotNull
        public static Future<Boolean> exec(CommandSender sender, String command) {
            return exec(sender, command, sender.getIOHandler());
        }

        /**
         * Execute command with sender executing and ioHandler receiving
         *
         * @param sender the executor
         * @param command the command CommandSender executes.
         * @param ioHandler the receiver
         * @return a Future representing pending completion of the command
         */
        @NotNull
        public static Future<Boolean> exec(CommandSender sender, String command, IOHandler ioHandler) {
            if (sender != CommandSender.CONSOLE)
                IOHandler.getConsoleIoHandler().output(sender + " exec: " + command);
            else Main.getLogger().consoleInput(command);
            List<String> args = Lists.newArrayList();
            StringBuilder stringBuilder = new StringBuilder();
            boolean stack = false;
            boolean ignore = false;
            for (char c : command.toCharArray()) {
                if (ignore) {
                    ignore = false;
                    switch (c) {
                        case 'a':stringBuilder.append((char)7);break;
                        case 'b':stringBuilder.append((char)8);break;
                        case 'f':stringBuilder.append((char)12);break;
                        case 'n':stringBuilder.append((char)10);break;
                        case 'r':stringBuilder.append((char)13);break;
                        case 't':stringBuilder.append((char)9);break;
                        case 'v':stringBuilder.append((char)11);break;
                        case '0':stringBuilder.append((char)0);break;
                        default:stringBuilder.append(c);break;
                    }
                } else if (c == '\\')
                    ignore = true;
                else if (c == ' ') {
                    if (!stack) {
                        if (stringBuilder.length() > 0){
                            args.add(stringBuilder.toString());
                            stringBuilder.delete(0, stringBuilder.length());
                        }
                    } else
                        stringBuilder.append(c);
                } else if (c == '"')
                    stack = !stack;
                else
                    stringBuilder.append(c);
            }
            if (stringBuilder.length() != 0)
                args.add(stringBuilder.toString());
            if (args.size() == 0)
                return CompletableFuture.completedFuture(false);
            String name = args.get(0);
            args.remove(0);
            return exec0(sender, name, args.toArray(new String[0]), ioHandler);
        }

        private static Future<Boolean> exec0(CommandSender sender, String command, String[] args, IOHandler ioHandler) {
            boolean flag = false;
            CombinedFuture ret = new CombinedFuture();
            for (Command com : Command.getCommands())
                if (com.getAliases().stream().anyMatch(i -> i.equalsIgnoreCase(command)) || com.getName().equalsIgnoreCase(command)) {
                    flag = true;
                    ret.combine(EXECUTOR.submit(() -> com.execute(sender, args, ioHandler)));
                }
            if (!flag && sender == CommandSender.CONSOLE)
                ioHandler.output("Unknown Command");
            return ret;
        }
    }

}

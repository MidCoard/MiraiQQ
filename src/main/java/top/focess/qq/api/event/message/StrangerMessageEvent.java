package top.focess.qq.api.event.message;

import top.focess.qq.api.bot.Bot;
import top.focess.qq.api.bot.Stranger;
import top.focess.qq.api.bot.message.MessageChain;
import top.focess.qq.api.bot.message.MessageSource;
import top.focess.qq.api.event.ListenerHandler;

/**
 * Called when a stranger chat with bot (this does not execute any commands)
 */
public class StrangerMessageEvent extends MessageEvent{

    private static final ListenerHandler LISTENER_HANDLER = new ListenerHandler();

    /**
     * The stranger who chats with bot
     */
    private final Stranger stranger;

    /**
     * Constructs a StrangerMessageEvent
     *
     * @param bot     the bot
     * @param message the chat message
     * @param stranger the stranger who chats with bot
     */
    public StrangerMessageEvent(Bot bot, MessageChain message, Stranger stranger, MessageSource source) {
        super(bot, message, source);
        this.stranger = stranger;
    }

    public Stranger getStranger() {
        return stranger;
    }
}

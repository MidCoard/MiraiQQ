package top.focess.qq.api.event.message;

import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.OnlineMessageSource;
import top.focess.qq.api.bot.Bot;
import top.focess.qq.api.bot.Group;
import top.focess.qq.api.bot.Member;
import top.focess.qq.api.event.ListenerHandler;

/**
 * Called when a member who chats in the group the bot is in (this does not execute any commands)
 */
public class GroupMessageEvent extends MessageEvent {

    private static final ListenerHandler LISTENER_HANDLER = new ListenerHandler();

    /**
     * The member who chats in the group the bot is in
     */
    private final Member member;

    /**
     * The source of the message
     */
    private final OnlineMessageSource.Incoming.FromGroup source;

    /**
     * Constructs a GroupMessageEvent
     *
     * @param bot the bot
     * @param member the one who chats in the group the bot is in
     * @param message the chat message
     * @param source the source of the message
     */
    public GroupMessageEvent(Bot bot, Member member, MessageChain message, OnlineMessageSource.Incoming.FromGroup source) {
        super(bot,message);
        this.member = member;
        this.source = source;
    }

    public Member getMember() {
        return member;
    }

    /**
     * Get the Group Mirai instance of the member
     *
     * @return the Group Mirai instance of the member
     */
    public Group getGroup() {
        return this.member.getGroup();
    }

    public OnlineMessageSource.Incoming.FromGroup getSource() {
        return source;
    }
}

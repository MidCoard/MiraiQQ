package com.focess.api.event;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a Member recall a message in a Group
 */
public class GroupRecallEvent extends Event{

    private static final ListenerHandler LISTENER_HANDLER = new ListenerHandler();

    /**
     * The member who send the message
     */
    private final Member member;
    /**
     * The message ids
     */
    private final int[] messageIds;
    /**
     * The member who recalls the message
     */
    private final Member operator;

    /**
     * Constructs a GroupRecallEvent
     *
     * @param member the member who send the message
     * @param messageIds the message ids
     * @param operator the member who recalls the message
     */
    public GroupRecallEvent(Member member, int[] messageIds, Member operator) {
        this.member = member;
        this.messageIds = messageIds;
        this.operator = operator;
    }

    @NotNull
    public Member getMember() {
        return member;
    }

    public int[] getMessageIds() {
        return messageIds;
    }

    /**
     * Get the Group where the member recalls the message
     *
     * @return the Group where the member recalls the message
     */
    @NotNull
    public Group getGroup() {
        return this.member.getGroup();
    }

    @NotNull
    public Member getOperator() {
        return operator;
    }
}

package com.focess.api.command;

import com.focess.Main;
import com.focess.api.util.IOHandler;
import com.focess.api.util.session.Session;
import com.focess.listener.ChatListener;
import com.google.common.collect.Maps;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.MemberPermission;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * This class present an executor to execute command. We can use it to distinguish different permissions.
 */
public class CommandSender {

    private static final Map<CommandSender, Session> SESSIONS = Maps.newHashMap();

    /**
     * Present CONSOLE or we call it Terminate
     */
    public static final CommandSender CONSOLE = new CommandSender();

    private final Member member;
    private final Friend friend;
    private final boolean isMember;
    private final boolean isFriend;
    private final MemberPermission permission;

    private CommandSender() {
        this.member = null;
        this.friend = null;
        this.isFriend = false;
        this.isMember = false;
        this.permission = MemberPermission.OWNER;
    }

    /**
     * Present Friend
     * @param friend The Mirai Friend Instance
     */
    public CommandSender(Friend friend) {
        this.member = null;
        this.friend = friend;
        this.isFriend = true;
        this.isMember = false;
        this.permission = MemberPermission.OWNER;
    }

    /**
     * Present Member in Group
     * @param member The Mirai Member Instance
     */
    public CommandSender(Member member) {
        this.member = member;
        this.friend = null;
        this.isMember = true;
        this.isFriend = false;
        this.permission = member.getPermission();
    }

    /**
     *
     * @return
     */
    @Nullable
    public Friend getFriend() {
        return friend;
    }

    public boolean isFriend() {
        return isFriend;
    }

    /**
     *
     *
     * @param permission the compared permission
     * @return {@link true} if the permission of this CommandSender is higher or equivalent to the compared permission, false otherwise
     */
    public boolean hasPermission(MemberPermission permission) {
        if (isAuthor())
            return true;
        switch (permission) {
            case MEMBER:
                return true;
            case ADMINISTRATOR:
                return this.getPermission() == MemberPermission.ADMINISTRATOR || this.getPermission() == MemberPermission.OWNER;
            case OWNER:
                return this.getPermission() == MemberPermission.OWNER;
        }
        return false;
    }

    @Nullable
    public Member getMember() {
        return member;
    }

    public boolean isMember() {
        return isMember;
    }

    public boolean isAuthor() {
        return this.isFriend ? this.friend.getId() == Main.getAuthorId() : isMember && this.member.getId() == Main.getAuthorId();
    }

    /**
     * Get Mirai Permission
     * @return Mirai Permission of this sender
     */
    public MemberPermission getPermission() {
        return permission;
    }

    public String toString() {
        if (this.isFriend())
            return friend.getNick() + "(" + this.friend.getId() + ")";
        else if (this.isMember)
            return member.getNameCard() + "(" + this.member.getId() + ")" + "[" + this.member.getGroup().getId() + "]";
        else return "CONSOLE";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandSender sender = (CommandSender) o;
        if (this.isMember() && sender.isMember()) {
            return sender.getMember().getGroup() == this.getMember().getGroup() && sender.getMember().getId() == this.getMember().getId();
        } else if (this.isFriend() && sender.isFriend()) {
            return sender.getFriend().getId() == this.getFriend().getId();
        } else return this.isConsole() && sender.isConsole();
    }

    @Override
    public int hashCode() {
        return Objects.hash(member == null ? null : member.getId(), friend == null ? null : friend.getId(), isMember, isFriend);
    }

    /**
     * indicate whether it is CONSOLE
     *
     * @return true if it is {@link CommandSender#CONSOLE}, false otherwise
     */
    public boolean isConsole() {
        return this == CONSOLE;
    }

    /**
     * Get the receiver by this CommandSender
     *
     * @return the receiver by this CommandSender
     */
    public IOHandler getIOHandler() {
        if (this.isConsole())
            return IOHandler.getConsoleIoHandler();
        return new IOHandler() {

            @Override
            public void output(String output) {
                if (isMember())
                    getMember().getGroup().sendMessage(output);
                else if (isFriend())
                    getFriend().sendMessage(output);
            }

            @Override
            public boolean hasInput(boolean flag) {
                ChatListener.registerInputListener(this, CommandSender.this, flag);
                while (!this.flag) ;
                return true;
            }

        };
    }

    /**
     * Execute command with this CommandSender
     *
     * @see com.focess.Main.CommandLine#exec(CommandSender, String)
     * @param command the command CommandSender execute
     */
    public void exec(String command) {
        Main.CommandLine.exec(this, command);
    }

    /**
     * Get Session of a special CommandSender. It can be used to store some data for future using.
     * But the data it stored will be lost after Bot exiting.
     *
     * @return Session of sender
     */
    public Session getSession() {
        if (SESSIONS.containsKey(this))
            return SESSIONS.get(this);
        else {
            Session session = new Session();
            SESSIONS.put(this,session);
            return session;
        }
    }

}

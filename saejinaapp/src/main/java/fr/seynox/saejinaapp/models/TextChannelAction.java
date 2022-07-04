package fr.seynox.saejinaapp.models;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import static net.dv8tion.jda.api.Permission.MANAGE_CHANNEL;

public enum TextChannelAction implements Selectable {

    SEND_TICKET_BUTTON("Send ticket creation button", (member, channel) -> channel.canTalk(member) && member.hasPermission(MANAGE_CHANNEL)),
    SEND_MESSAGE("Send message", (member, channel) -> channel.canTalk(member)),
    SEND_ROLE_BUTTON("Send role assignment button", (member, channel) -> channel.canTalk(member) && member.hasPermission(MANAGE_CHANNEL));

    private final String name;
    private final PermissionValidation validation;
    TextChannelAction(String name, PermissionValidation validation) {
        this.name = name;
        this.validation = validation;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return this.toString().toLowerCase();
    }

    public boolean isAllowed(Member member, TextChannel channel) {
        return validation.isAllowed(member, channel);
    }


    private interface PermissionValidation {
        boolean isAllowed(Member member, TextChannel channel);
    }
}

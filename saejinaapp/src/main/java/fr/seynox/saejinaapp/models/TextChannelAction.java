package fr.seynox.saejinaapp.models;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public enum TextChannelAction implements Selectable {

    SEND_TICKET_BUTTON("Send ticket creation button", (member, channel) -> member.hasPermission(Permission.MANAGE_SERVER)),
    SEND_MESSAGE("Send message", (member, channel) -> channel.canTalk(member));

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

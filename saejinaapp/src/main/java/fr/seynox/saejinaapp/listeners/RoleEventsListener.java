package fr.seynox.saejinaapp.listeners;

import fr.seynox.saejinaapp.exceptions.DiscordInteractionException;
import fr.seynox.saejinaapp.services.RoleService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class RoleEventsListener {

    private static final Pattern assignPattern = Pattern.compile("role-assign#\\d*");

    private final RoleService service;

    public RoleEventsListener(RoleService service) {
        this.service = service;
    }

    /**
     * Triggered when a user presses a button.
     * Used to handle role assignment button
     */
    @SubscribeEvent
    public void onButtonPress(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if(assignPattern.matcher(buttonId).matches()) {
            String roleIdText = buttonId.split("#")[1];
            Long roleId = Long.parseLong(roleIdText);

            toggleRole(roleId, event);
        }

    }

    /**
     * Toggles the given role to the member that clicked the button
     * @param roleId The role to toggle
     * @param replyCallback The callback used to get and reply to the member
     */
    public void toggleRole(Long roleId, IReplyCallback replyCallback) {

        Member member = replyCallback.getMember();
        String message;
        try {
            boolean wasRoleAdded = service.toggleRoleForMember(member, roleId);
            String toggleAction = wasRoleAdded ? "added" : "removed";
            message = "Successfully %s the role".formatted(toggleAction);
        } catch(DiscordInteractionException exception) {
            message = exception.getMessage();
        }

        replyCallback.reply(message)
                .setEphemeral(true)
                .queue();
    }
}

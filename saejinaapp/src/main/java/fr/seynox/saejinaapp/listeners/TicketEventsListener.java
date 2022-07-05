package fr.seynox.saejinaapp.listeners;

import fr.seynox.saejinaapp.exceptions.DiscordInteractionException;
import fr.seynox.saejinaapp.services.TicketService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static fr.seynox.saejinaapp.services.TicketService.*;

@Component
public class TicketEventsListener {

    private static final Pattern invitePattern = Pattern.compile("ticket-invite#\\d*");

    private final TicketService service;

    public TicketEventsListener(TicketService service) {
        this.service = service;
    }

    /**
     *  Triggered when a user presses a button.
     *  Used to handle tickets buttons (Create, Close, Invite)
     */
    @SubscribeEvent
    public void onButtonPress(ButtonInteractionEvent event) {

        String buttonId = event.getComponentId();
        switch(buttonId) {
            // Create ticket
            case TICKET_CREATION_ID -> showTicketCreationForm(event);

            // Ask close ticket confirmation
            case TICKET_CLOSE_ID -> {
                Button confirmationButton = Button.of(ButtonStyle.DANGER, TICKET_CLOSE_CONFIRM_ID, "Are you sure ?");
                event.editButton(confirmationButton).queue();
            }

            // Close ticket
            case TICKET_CLOSE_CONFIRM_ID -> {
                event.reply("Closing the ticket ! Deleting channel in 30sec...").queue();
                CompletableFuture.delayedExecutor(30L, TimeUnit.SECONDS)
                        .execute(() -> event.getTextChannel().delete().queue());
            }

            default -> {
                // Invite user to ticket channel
                if(invitePattern.matcher(buttonId).matches()) {
                    String userId = buttonId.split("#")[1];
                    inviteMemberToChannel(userId, event);
                }
            }

        }
    }

    /**
     * Triggered when a user submits a modal (form).
     * Used to handle ticket submission
     */
    @SubscribeEvent
    public void onModalSubmit(ModalInteractionEvent event) {

        String modalId = event.getModalId();
        if(modalId.equals(TICKET_CREATION_ID)) {
            CompletableFuture.runAsync(() -> submitTicketToServer(event));
        }

    }

    /**
     * Display a ticket creation modal (form) to the member that clicked the button
     */
    public void showTicketCreationForm(ButtonInteractionEvent event) {
        Member member = event.getMember();
        Button button = event.getButton();
        try {
            Modal modal = service.getTicketCreationForm(member, button);
            event.replyModal(modal).queue();
        } catch(DiscordInteractionException exception) {
            String message = exception.getMessage();
            event.reply(message)
                    .setEphemeral(true)
                    .queue();
        }
    }

    /**
     * Allow the given user to access the button's channel
     * @param userId The user to invite
     */
    public void inviteMemberToChannel(String userId, ButtonInteractionEvent event) {
        Button button = event.getButton();
        TextChannel channel = event.getTextChannel();

        try {
            service.inviteUserToTicketChannel(userId, channel);
            String message = "The ticket owner (%s) has been invited to the channel !"
                    .formatted("<@" + userId + '>');
            event.reply(message).queue();

        } catch(DiscordInteractionException exception) {
            String errorMessage = exception.getMessage();
            event.reply(errorMessage)
                    .setEphemeral(true)
                    .queue();

            return;
        }

        // Disable button
        event.editButton(button.asDisabled()).queue();
    }

    /**
     *  Submits a ticket to the server via a modal (form)
     */
    public void submitTicketToServer(ModalInteractionEvent event) {
        Member member = event.getMember();
        if(member == null) {
            event.reply("Error ! Tickets can only be submitted in servers")
                    .setEphemeral(true)
                    .queue();

            return;
        }

        Guild guild = member.getGuild();

        String message;
        try {
            TextChannel ticketChannel = service.createTicketChannel(guild);
            service.sendTicketToChannel(member, ticketChannel, event);
            message = "Your ticket was submitted !";
        } catch(InsufficientPermissionException exception) {
            message = "Error ! I do not have the permissions required to create channels. Please contact a server administrator";
        } catch(NullPointerException exception) {
            message = "Error ! The ticket you submitted is invalid";
        }

        event.reply(message)
                .setEphemeral(true)
                .queue();
    }

}

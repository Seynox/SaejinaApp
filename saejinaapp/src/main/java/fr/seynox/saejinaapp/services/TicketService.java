package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import static fr.seynox.saejinaapp.models.TextChannelAction.SEND_TICKET_BUTTON;

@Service
public class TicketService {

    public static final String TICKET_CREATION_ID = "ticket-creation";
    public static final String TICKET_INVITE_ID_TEMPLATE =  "ticket-invite#%s";
    public static final String TICKET_CLOSE_ID =  "ticket-close";
    public static final String TICKET_CLOSE_CONFIRM_ID =  "ticket-close-confirm";
    public static final String TICKETS_CATEGORY_NAME = "\uD83C\uDFAB | Tickets";

    /**
     * Send a button used to create tickets
     * WARNING ! This method does not check if the channel is writable for the bot/user, nor the length of the label
     * @param member The user sending the buttons
     * @param channel The channel to send the button to
     * @param label The button's label
     * @throws PermissionException If the member is not allowed to send ticket buttons on the server
     */
    public void sendTicketButtonInChannel(Member member, TextChannel channel, String label) {

        boolean isAllowed = SEND_TICKET_BUTTON.isAllowed(member, channel);
        if(!isAllowed) {
            throw new PermissionException("You do not have the permission to send ticket buttons.");
        }

        Emoji ticketEmoji = Emoji.fromUnicode("U+1F39F");
        Button ticketButton = Button.of(ButtonStyle.SECONDARY, TICKET_CREATION_ID, label, ticketEmoji);

        channel.sendMessage("⌄")
                .setActionRow(ticketButton)
                .queue();
    }

    /**
     * Show the ticket creation form in Discord
     * @param member The member to show the form to
     * @param button The button clicked
     * @param interaction The member interaction. Used to reply error/form
     */
    public void showTicketCreationForm(Member member, Button button, ComponentInteraction interaction) {
        if(member == null) {
            interaction.reply("Error ! Ticket buttons are only usable in servers")
                    .setEphemeral(true)
                    .queue();

            return;
        }

        TextInput subject = TextInput.create("subject", "Subject", TextInputStyle.SHORT)
                .setPlaceholder("Subject of this ticket")
                .setRequiredRange(10, 100)
                .build();

        TextInput message = TextInput.create("body", "Body", TextInputStyle.PARAGRAPH)
                .setMinLength(30)
                .setMaxLength(1000)
                .build();

        String buttonLabel = button.getLabel();
        Modal form = Modal.create(TICKET_CREATION_ID, buttonLabel)
                .addActionRows(ActionRow.of(subject), ActionRow.of(message))
                .build();

        interaction.replyModal(form).queue();
    }

    /**
     * Create a new ticket channel.
     * Will create a new channel category if it doesn't exist
     * @param guild The guild in which the ticket channel needs to be created
     * @return The created text channel
     */
    public TextChannel createTicketChannel(Guild guild) {
        // Get or create tickets category
        Role everyoneRole = guild.getPublicRole();

        Category ticketsCategory = guild.getCategoriesByName(TICKETS_CATEGORY_NAME, false).stream()
                .findFirst()
                .orElseGet(() -> guild.createCategory(TICKETS_CATEGORY_NAME)
                        .addPermissionOverride(everyoneRole, List.of(), List.of(Permission.VIEW_CHANNEL))
                        .complete());

        // Get ticket channel name
        int channelsInCategory = ticketsCategory.getTextChannels().size();
        int ticketNumber = channelsInCategory + 1;
        String ticketName = "ticket-" + ticketNumber;

        // Create channel
        return ticketsCategory.createTextChannel(ticketName).complete();
    }

    /**
     * Send the created ticket in the given channel
     * @param member The member creating the ticket
     * @param ticketChannel The channel to send the ticket to (Can be created using {@link  TicketService#createTicketChannel(Guild)})
     * @param event The modal event. Used to get the ticket content and reply
     */
    public void sendTicketToChannel(Member member, TextChannel ticketChannel, ModalInteractionEvent event) {
        // Make ticket embed
        String senderName = "%s (@%s)".formatted(member.getEffectiveName(), member.getId());
        String senderAvatar = member.getEffectiveAvatarUrl();
        String subject = Objects.requireNonNull(event.getValue("subject")).getAsString();
        String body = Objects.requireNonNull(event.getValue("body")).getAsString();

        MessageEmbed ticketEmbed = new EmbedBuilder()
                .setTitle(subject)
                .setDescription(body)
                .setAuthor(senderName, null, senderAvatar)
                .setColor(Color.ORANGE)
                .build();

        // Make buttons
        Button userInvite = Button.of(ButtonStyle.SECONDARY, TICKET_INVITE_ID_TEMPLATE.formatted(member.getId()), "Invite the user to this channel");
        Button closeTicket = Button.of(ButtonStyle.DANGER, TICKET_CLOSE_ID, "Close this ticket");

        // Send ticket embed and buttons
        ticketChannel.sendMessageEmbeds(ticketEmbed)
                .setActionRows(ActionRow.of(userInvite, closeTicket))
                .queue();

        // Send success message
        event.reply("Your ticket was submitted !")
                .setEphemeral(true)
                .queue();
    }

    /**
     * Allow a user to see the ticket channel
     * @param userId The user to invite
     * @param channel The ticket channel the user is invited to
     * @param interaction The interaction with the member that invites the user. Used to reply error or success message
     */
    public void inviteUserToTicketChannel(String userId, TextChannel channel, ComponentInteraction interaction) {
        // Check user
        Guild guild = channel.getGuild();
        Member member = guild.retrieveMemberById(userId).complete();
        if(member == null) {
            interaction.reply("Error ! The user could not be found in the server")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Add user to channel
        channel.getManager()
                .putMemberPermissionOverride(member.getIdLong(), List.of(Permission.VIEW_CHANNEL), List.of())
                .queue();

        // Send success message
        String successMessage = "The ticket owner (%s) has been invited to the channel !"
                .formatted(member.getAsMention());

        interaction.reply(successMessage).queue();
    }

}

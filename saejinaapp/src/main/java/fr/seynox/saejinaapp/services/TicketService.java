package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.DiscordInteractionException;
import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.utils.ButtonUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.ModalInteraction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
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

    private final ButtonUtils buttonUtils;

    public TicketService(ButtonUtils buttonUtils) {
        this.buttonUtils = buttonUtils;
    }

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

        buttonUtils.sendOrAppendButton(channel, ticketButton);
    }

    /**
     * Create a ticket creation form
     * @param member The member to show the form to
     * @param button The button clicked
     * @throws DiscordInteractionException If the member is null
     * @return The ticket creation form
     */
    public Modal getTicketCreationForm(Member member, Button button) throws DiscordInteractionException {
        if(member == null) {
            throw new DiscordInteractionException("Error ! Ticket buttons are only usable in servers");
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
        return Modal.create(TICKET_CREATION_ID, buttonLabel)
                .addActionRows(ActionRow.of(subject), ActionRow.of(message))
                .build();
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
     * @param interaction The modal interaction. Used to get the ticket content
     */
    public void sendTicketToChannel(Member member, TextChannel ticketChannel, ModalInteraction interaction) {
        // Make ticket embed
        String senderName = "%s (@%s)".formatted(member.getEffectiveName(), member.getId());
        String senderAvatar = member.getEffectiveAvatarUrl();
        String subject = Objects.requireNonNull(interaction.getValue("subject")).getAsString();
        String body = Objects.requireNonNull(interaction.getValue("body")).getAsString();

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
    }

    /**
     * Allow a user to see the ticket channel
     * @param userId The user to invite
     * @param channel The ticket channel the user is invited to
     * @throws DiscordInteractionException If the user could not be found in the server
     */
    public void inviteUserToTicketChannel(String userId, TextChannel channel) throws DiscordInteractionException {
        // Check user
        Guild guild = channel.getGuild();
        Member member = guild.retrieveMemberById(userId).complete();
        if(member == null) {
            throw new DiscordInteractionException("Error ! The user could not be found in the server");
        }

        // Add user to channel
        channel.getManager()
                .putMemberPermissionOverride(member.getIdLong(), List.of(Permission.VIEW_CHANNEL), List.of())
                .queue();
    }

}

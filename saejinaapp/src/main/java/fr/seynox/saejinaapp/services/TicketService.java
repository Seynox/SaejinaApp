package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.springframework.stereotype.Service;

import static fr.seynox.saejinaapp.models.TextChannelAction.SEND_TICKET_BUTTON;

@Service
public class TicketService {

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
        Button ticketButton = Button.of(ButtonStyle.SECONDARY, "ticket-creation", label, ticketEmoji);

        channel.sendMessage("⌄")
                .setActionRow(ticketButton)
                .queue();
    }

}

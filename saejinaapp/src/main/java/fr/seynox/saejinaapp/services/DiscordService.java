package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.DiscordTextChannel;
import fr.seynox.saejinaapp.models.TextChannelAction;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static fr.seynox.saejinaapp.models.TextChannelAction.SEND_TICKET_BUTTON;
import static net.dv8tion.jda.api.entities.Message.MentionType.EVERYONE;
import static net.dv8tion.jda.api.entities.Message.MentionType.HERE;

@Service
public class DiscordService {

    private final JDA jda;

    public DiscordService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Get all mutual servers for the given user
     * @return An empty list if the user is null
     */
    public List<Server> getUserServers(String userId) {
        User user = jda.retrieveUserById(userId).complete();
        if(user == null) {
            return List.of();
        }

        return user.getMutualGuilds().stream()
                .map(guild -> new Server(guild.getName(), guild.getIconUrl(), guild.getIdLong()))
                .toList();
    }

    /**
     * Get all server text channels that are visible to the given user
     * @return A list of text channels visible to the user
     */
    public List<DiscordTextChannel> getVisibleServerTextChannels(@NonNull Member member) {
        Guild server = member.getGuild();

        return server.getTextChannels().stream()
                .filter(member::hasAccess)
                .map(channel -> new DiscordTextChannel(channel.getIdLong(), channel.getName()))
                .toList();
    }

    /**
     * Get a list of all possible actions for the user on the channel
     * @param member The user get the actions
     * @param channel The channel the action is being applied to
     * @return The list of allowed channel actions
     */
    public List<TextChannelAction> getPossibleActionsForChannel(Member member, TextChannel channel) {
        return Arrays.stream(TextChannelAction.values())
                .filter(action -> action.isAllowed(member, channel))
                .toList();
    }

    /**
     * Send a message in the given channel.
     * WARNING ! This method does not check if the channel is writable for the bot/user, nor the length of the message
     * @param member The member sending the message
     * @param channel The channel to send the message to
     * @param content The content of the message
     * @throws PermissionException If the member is trying to send an @everyone without the required permission
     */
    public void sendMessageInChannel(Member member, TextChannel channel, String content) {

        boolean mentionsEveryone = EVERYONE.getPattern()
                .matcher(content)
                .find();

        boolean mentionsHere = HERE.getPattern()
                .matcher(content)
                .find();

        if(mentionsEveryone || mentionsHere) {
            boolean canMentionEveryone = member.hasPermission(Permission.MESSAGE_MENTION_EVERYONE);
            if(!canMentionEveryone)  {
                throw new PermissionException("You do not have the permission to mention everyone");
            }
        }

        channel.sendMessage(content).queue();
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
        Button ticketButton = Button.of(ButtonStyle.SECONDARY, "ticket-creation", label, ticketEmoji);

        channel.sendMessage("⌄")
                .setActionRow(ticketButton)
                .queue();
    }
}

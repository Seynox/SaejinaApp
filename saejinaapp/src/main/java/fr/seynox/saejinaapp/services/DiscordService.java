package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannelAction;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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

}

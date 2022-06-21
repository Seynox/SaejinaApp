package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannel;
import fr.seynox.saejinaapp.models.TextChannelAction;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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
    public List<TextChannel> getVisibleServerTextChannels(@NonNull Member member) {
        Guild server = member.getGuild();

        return server.getTextChannels().stream()
                .filter(member::hasAccess)
                .map(channel -> new TextChannel(channel.getIdLong(), channel.getName()))
                .toList();
    }

    /**
     * Get a list of all possible actions for the user on the channel
     * @param member The user get the actions
     * @param channel The channel the action is being applied to
     * @return The list of allowed channel actions
     */
    public List<TextChannelAction> getPossibleActionsForChannel(Member member, net.dv8tion.jda.api.entities.TextChannel channel) {
        return Arrays.stream(TextChannelAction.values())
                .filter(action -> action.isAllowed(member, channel))
                .toList();
    }
}

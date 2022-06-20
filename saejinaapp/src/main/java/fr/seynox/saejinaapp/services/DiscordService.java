package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.ServerNotAccessibleException;
import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannel;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Service;

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
     * @throws ServerNotAccessibleException When the user is null, or when the bot or user does not have access to the server
     * @return A list of text channels visible to the user
     */
    public List<TextChannel> getVisibleServerTextChannels(String userId, Long serverId) {
        Guild server = jda.getGuildById(serverId);
        if(server == null) {
            throw new ServerNotAccessibleException();
        }

        Member member = server.retrieveMemberById(userId).complete();
        if(member == null) {
            throw new ServerNotAccessibleException();
        }

        return server.getTextChannels().stream()
                .filter(channel -> member.hasPermission(channel, Permission.VIEW_CHANNEL))
                .map(channel -> new TextChannel(channel.getIdLong(), channel.getName()))
                .toList();
    }
}

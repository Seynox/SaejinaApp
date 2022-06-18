package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannel;
import net.dv8tion.jda.api.JDA;
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

    public List<Server> getUserServers(String userId) {
        User user = jda.retrieveUserById(userId).complete();
        if(user == null) {
            return List.of();
        }

        return user.getMutualGuilds().stream()
                .map(guild -> new Server(guild.getName(), guild.getIconUrl(), guild.getIdLong()))
                .toList();
    }

    public boolean isUserInServer(String userId, Long serverId) {
        Guild server = jda.getGuildById(serverId);

        if(server != null) {
            Member member = server.retrieveMemberById(userId).complete();
            return member != null;
        }

        return false;
    }

    public List<TextChannel> getServerTextChannels(Long serverId) {
        Guild server = jda.getGuildById(serverId);
        if(server == null) {
            return List.of();
        }

        return server.getTextChannels().stream()
                .map(channel -> new TextChannel(channel.getIdLong(), channel.getName()))
                .toList();
    }
}

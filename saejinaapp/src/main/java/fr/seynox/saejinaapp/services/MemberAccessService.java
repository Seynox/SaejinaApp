package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import fr.seynox.saejinaapp.models.SelectableImpl;
import fr.seynox.saejinaapp.models.Selectable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberAccessService {

    private final JDA jda;

    public MemberAccessService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Get the user as member of the given server
     * @param userId The user to get as a member
     * @throws ResourceNotAccessibleException If the bot/user does not have access to the server
     */
    public Member getServerMember(String userId, Long serverId) {

        Guild server = jda.getGuildById(serverId);
        if(server != null) {

            Member member = server.retrieveMemberById(userId).complete();
            if(member != null) {
                return member;
            }
        }

        throw new ResourceNotAccessibleException();
    }

    /**
     * Get the server text channel if accessible to the user
     * @param member The user getting the channel
     * @param channelId The channel to get
     * @throws ResourceNotAccessibleException If the bot/user does not have access to the given channel
     * @return The server text channel
     */
    public TextChannel getServerTextChannel(Member member, Long channelId) {

        Guild guild = member.getGuild();
        TextChannel channel = guild.getTextChannelById(channelId);

        boolean isChannelAccessible = channel != null && member.hasAccess(channel);
        if(!isChannelAccessible) {
            throw new ResourceNotAccessibleException();
        }

        return channel;
    }

    /**
     * Get all server text channels that are accessible to the given user
     * @return A list of text channels visible to the user
     */
    public List<Selectable> getServerTextChannels(@NonNull Member member) {
        Guild server = member.getGuild();

        return server.getTextChannels().stream()
                .filter(member::hasAccess)
                .map(channel -> (Selectable) new SelectableImpl(channel.getIdLong(), channel.getName()))
                .toList();
    }

    /**
     * Get the server text channel if accessible and writable to the user
     * @param member The user getting the channel
     * @param channelId The channel to get
     * @throws ResourceNotAccessibleException If the bot/user does not have access to the given channel
     * @throws PermissionException If the bot/user does not have the permission to talk in the given channel
     * @return The server text channel
     */
    public TextChannel getWritableServerTextChannel(Member member, Long channelId) {

        TextChannel channel = getServerTextChannel(member, channelId);

        if(!channel.canTalk(member)) {
            throw new PermissionException("You do not have the permission to talk in this channel");
        }
        if(!channel.canTalk()) {
            throw new PermissionException("I do not have the permission to talk in this channel. Please contact the server administrators");
        }

        return channel;
    }


}

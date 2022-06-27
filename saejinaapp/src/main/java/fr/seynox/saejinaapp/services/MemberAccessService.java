package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.stereotype.Service;

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


}

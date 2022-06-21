package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.ServerNotAccessibleException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class MemberAccessServiceTests {

    private JDA jda;
    private MemberAccessService service;

    @BeforeEach
    void initTest() {
        this.jda = Mockito.mock(JDA.class);
        this.service = new MemberAccessService(jda);
    }

    @Test
    void getServerMemberTest() {
        // GIVEN
        String userId = "123456";
        long serverId = 6543214;

        Guild guild = Mockito.mock(Guild.class);
        Member member = Mockito.mock(Member.class);

        when(jda.getGuildById(serverId)).thenReturn(guild);
        when(guild.retrieveMemberById(userId)).thenReturn(new CompletedRestAction<>(null, member));
        // WHEN
        service.getServerMember(userId, serverId);

        // THEN
        verify(jda, times(1)).getGuildById(serverId);
        verify(guild, times(1)).retrieveMemberById(userId);
    }

    @Test
    void getNullServerMemberTest() {
        // GIVEN
        String userId = "123456";
        long serverId = 6543214;

        when(jda.getGuildById(serverId)).thenReturn(null);
        // WHEN
        assertThatExceptionOfType(ServerNotAccessibleException.class)
                .isThrownBy(() -> service.getServerMember(userId, serverId));

        // THEN
        verify(jda, times(1)).getGuildById(serverId);
    }

    @Test
    void getServerNullMemberTest() {
        // GIVEN
        String userId = "123456";
        long serverId = 6543214;

        Guild guild = Mockito.mock(Guild.class);

        when(jda.getGuildById(serverId)).thenReturn(guild);
        when(guild.retrieveMemberById(userId)).thenReturn(new CompletedRestAction<>(null, null));
        // WHEN
        assertThatExceptionOfType(ServerNotAccessibleException.class)
                .isThrownBy(() -> service.getServerMember(userId, serverId));

        // THEN
        verify(jda, times(1)).getGuildById(serverId);
        verify(guild, times(1)).retrieveMemberById(userId);
    }

    @Test
    void getServerTextChannelTest() {
        // GIVEN
        String channelId = "123456";

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(member.getGuild()).thenReturn(guild);
        when(member.hasAccess(channel)).thenReturn(true);
        when(guild.getTextChannelById(channelId)).thenReturn(channel);
        // WHEN
        service.getServerTextChannel(member, channelId);

        // THEN
        verify(member, times(1)).getGuild();
        verify(member, times(1)).hasAccess(channel);
        verify(guild, times(1)).getTextChannelById(channelId);
    }

    @Test
    void refuseServerInvisibleTextChannelTest() {
        // GIVEN
        String channelId = "123456";

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(member.getGuild()).thenReturn(guild);
        when(member.hasAccess(channel)).thenReturn(false);
        when(guild.getTextChannelById(channelId)).thenReturn(channel);
        // WHEN
        assertThatExceptionOfType(ServerNotAccessibleException.class)
                .isThrownBy(() -> service.getServerTextChannel(member, channelId));

        // THEN
        verify(member, times(1)).getGuild();
        verify(member, times(1)).hasAccess(channel);
        verify(guild, times(1)).getTextChannelById(channelId);
    }

    @Test
    void refuseServerUnknownTextChannel() {
        // GIVEN
        String channelId = "123456";

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);

        when(member.getGuild()).thenReturn(guild);
        when(guild.getTextChannelById(channelId)).thenReturn(null);
        // WHEN
        assertThatExceptionOfType(ServerNotAccessibleException.class)
                .isThrownBy(() -> service.getServerTextChannel(member, channelId));

        // THEN
        verify(member, times(1)).getGuild();
        verify(member, never()).hasAccess(any());
        verify(guild, times(1)).getTextChannelById(channelId);
    }

}

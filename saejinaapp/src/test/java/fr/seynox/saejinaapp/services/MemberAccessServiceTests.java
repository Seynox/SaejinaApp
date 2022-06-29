package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
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
        verify(jda).getGuildById(serverId);
        verify(guild).retrieveMemberById(userId);
    }

    @Test
    void getNullServerMemberTest() {
        // GIVEN
        String userId = "123456";
        long serverId = 6543214;

        when(jda.getGuildById(serverId)).thenReturn(null);
        // WHEN
        assertThatExceptionOfType(ResourceNotAccessibleException.class)
                .isThrownBy(() -> service.getServerMember(userId, serverId));

        // THEN
        verify(jda).getGuildById(serverId);
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
        assertThatExceptionOfType(ResourceNotAccessibleException.class)
                .isThrownBy(() -> service.getServerMember(userId, serverId));

        // THEN
        verify(jda).getGuildById(serverId);
        verify(guild).retrieveMemberById(userId);
    }

    @Test
    void getServerTextChannelTest() {
        // GIVEN
        long channelId = 123456;

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(member.getGuild()).thenReturn(guild);
        when(member.hasAccess(channel)).thenReturn(true);
        when(guild.getTextChannelById(channelId)).thenReturn(channel);
        // WHEN
        service.getServerTextChannel(member, channelId);

        // THEN
        verify(member).getGuild();
        verify(member).hasAccess(channel);
        verify(guild).getTextChannelById(channelId);
    }

    @Test
    void refuseServerInvisibleTextChannelTest() {
        // GIVEN
        long channelId = 123456;

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(member.getGuild()).thenReturn(guild);
        when(member.hasAccess(channel)).thenReturn(false);
        when(guild.getTextChannelById(channelId)).thenReturn(channel);
        // WHEN
        assertThatExceptionOfType(ResourceNotAccessibleException.class)
                .isThrownBy(() -> service.getServerTextChannel(member, channelId));

        // THEN
        verify(member).getGuild();
        verify(member).hasAccess(channel);
        verify(guild).getTextChannelById(channelId);
    }

    @Test
    void refuseServerUnknownTextChannel() {
        // GIVEN
        long channelId = 123456;

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);

        when(member.getGuild()).thenReturn(guild);
        when(guild.getTextChannelById(channelId)).thenReturn(null);
        // WHEN
        assertThatExceptionOfType(ResourceNotAccessibleException.class)
                .isThrownBy(() -> service.getServerTextChannel(member, channelId));

        // THEN
        verify(member).getGuild();
        verify(member, never()).hasAccess(any());
        verify(guild).getTextChannelById(channelId);
    }

    @Test
    void getWritableServerTextChannelTest() {
        // GIVEN
        long channelId = 123456;

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(member.getGuild()).thenReturn(guild);
        when(member.hasAccess(channel)).thenReturn(true);
        when(guild.getTextChannelById(channelId)).thenReturn(channel);
        when(channel.canTalk()).thenReturn(true);
        when(channel.canTalk(member)).thenReturn(true);
        // WHEN
        service.getWritableServerTextChannel(member, channelId);

        // THEN
        verify(member).getGuild();
        verify(member).hasAccess(channel);
        verify(guild).getTextChannelById(channelId);
        verify(channel).canTalk(member);
        verify(channel).canTalk();
    }

    @Test
    void refuseUserNonWritableServerTextChannelTest() {
        // GIVEN
        long channelId = 123456;

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(member.getGuild()).thenReturn(guild);
        when(member.hasAccess(channel)).thenReturn(true);
        when(guild.getTextChannelById(channelId)).thenReturn(channel);
        when(channel.canTalk(member)).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.getWritableServerTextChannel(member, channelId))
                .withMessage("You do not have the permission to talk in this channel");

        // THEN
        verify(member).getGuild();
        verify(member).hasAccess(channel);
        verify(guild).getTextChannelById(channelId);
        verify(channel).canTalk(member);
        verify(channel, never()).canTalk();
    }

    @Test
    void refuseBotNonWritableServerTextChannelTest() {
        // GIVEN
        long channelId = 123456;

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(member.getGuild()).thenReturn(guild);
        when(member.hasAccess(channel)).thenReturn(true);
        when(guild.getTextChannelById(channelId)).thenReturn(channel);
        when(channel.canTalk(member)).thenReturn(true);
        when(channel.canTalk()).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.getWritableServerTextChannel(member, channelId))
                .withMessage("I do not have the permission to talk in this channel. Please contact the server administrators");

        // THEN
        verify(member).getGuild();
        verify(member).hasAccess(channel);
        verify(guild).getTextChannelById(channelId);
        verify(channel).canTalk(member);
        verify(channel).canTalk();
    }

}

package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import fr.seynox.saejinaapp.models.SelectableImpl;
import fr.seynox.saejinaapp.models.Selectable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class MemberAccessServiceTests {

    private JDA jda;
    private MemberAccessService service;

    private Member member;
    private Guild guild;

    @BeforeEach
    void initTest() {
        jda = Mockito.mock(JDA.class);
        service = new MemberAccessService(jda);

        member = Mockito.mock(Member.class);
        guild = Mockito.mock(Guild.class);
    }

    @Test
    void getServerMemberTest() {
        // GIVEN
        String userId = "123456";
        long serverId = 6543214;

        RestAction<Member> action = Mockito.mock(RestAction.class);

        when(jda.getGuildById(serverId)).thenReturn(guild);
        when(guild.retrieveMemberById(userId)).thenReturn(action);
        when(action.complete()).thenReturn(member);
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

        RestAction<Member> action = Mockito.mock(RestAction.class);

        when(jda.getGuildById(serverId)).thenReturn(guild);
        when(guild.retrieveMemberById(userId)).thenReturn(action);
        when(action.complete()).thenReturn(null);
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
    void getVisibleServerTextChannelsTest() {
        // GIVEN
        TextChannel channelOne = Mockito.mock(TextChannel.class);
        TextChannel channelTwo = Mockito.mock(TextChannel.class);

        List<TextChannel> channels = List.of(channelOne, channelTwo);

        List<SelectableImpl> expected = List.of(
                new SelectableImpl(1L, "Channel One"),
                new SelectableImpl(2L, "Channel Two")
        );

        List<Selectable> result;

        when(channelOne.getName()).thenReturn("Channel One");
        when(channelOne.getIdLong()).thenReturn(1L);
        when(channelTwo.getName()).thenReturn("Channel Two");
        when(channelTwo.getIdLong()).thenReturn(2L);

        when(member.getGuild()).thenReturn(guild);
        when(guild.getTextChannels()).thenReturn(channels);
        when(member.hasAccess(any())).thenReturn(true);
        // WHEN
        result = service.getServerTextChannels(member);

        // THEN
        verify(member).getGuild();
        verify(guild).getTextChannels();
        verify(member, times(2)).hasAccess(any());
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void getVisibleServerTextChannelsWithNoChannelTest() {
        // GIVEN
        List<Selectable> result;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getTextChannels()).thenReturn(List.of());
        // WHEN
        result = service.getServerTextChannels(member);

        // THEN
        verify(member).getGuild();
        verify(guild).getTextChannels();
        assertThat(result).isEmpty();
    }

    @Test
    void filterInvisibleChannelsTest() {
        // GIVEN
        TextChannel visibleChannel = Mockito.mock(TextChannel.class);
        TextChannel invisibleChannel = Mockito.mock(TextChannel.class);

        List<TextChannel> channels = List.of(visibleChannel, invisibleChannel);

        List<SelectableImpl> expected = List.of(new SelectableImpl(1L, "Channel One"));

        List<Selectable> result;

        when(visibleChannel.getName()).thenReturn("Channel One");
        when(visibleChannel.getIdLong()).thenReturn(1L);
        when(invisibleChannel.getName()).thenReturn("Invisible Channel");
        when(invisibleChannel.getIdLong()).thenReturn(2L);

        when(member.getGuild()).thenReturn(guild);
        when(guild.getTextChannels()).thenReturn(channels);
        when(member.hasAccess(invisibleChannel)).thenReturn(false);
        when(member.hasAccess(visibleChannel)).thenReturn(true);
        // WHEN
        result = service.getServerTextChannels(member);

        // THEN
        verify(member).getGuild();
        verify(guild).getTextChannels();
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void getWritableServerTextChannelTest() {
        // GIVEN
        long channelId = 123456;

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

    @Test
    void getAssignableServerRoleTest() {
        // GIVEN
        long roleId = 123456789;
        Role role = Mockito.mock(Role.class);

        Role result;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getRoleById(anyLong())).thenReturn(role);
        when(member.canInteract(any(Role.class))).thenReturn(true);
        // WHEN
        result = service.getAssignableServerRole(member, roleId);

        // THEN
        verify(guild).getRoleById(roleId);
        verify(member).canInteract(role);
        assertThat(result).isEqualTo(role);
    }

    @Test
    void refuseUnknownServerRoleTest() {
        // GIVEN
        long roleId = 123456789;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getRoleById(anyLong())).thenReturn(null);
        // WHEN
        assertThatExceptionOfType(ResourceNotAccessibleException.class)
                .isThrownBy(() -> service.getAssignableServerRole(member, roleId));

        // THEN
        verify(guild).getRoleById(roleId);
    }

    @Test
    void refuseNonAssignableServerRoleTest() {
        // GIVEN
        long roleId = 123456789;
        Role role = Mockito.mock(Role.class);

        when(member.getGuild()).thenReturn(guild);
        when(guild.getRoleById(anyLong())).thenReturn(role);
        when(member.canInteract(any(Role.class))).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.getAssignableServerRole(member, roleId));

        // THEN
        verify(guild).getRoleById(roleId);
        verify(member).canInteract(role);
    }

}

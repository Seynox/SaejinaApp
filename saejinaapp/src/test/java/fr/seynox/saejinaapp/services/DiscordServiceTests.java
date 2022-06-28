package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannel;
import fr.seynox.saejinaapp.models.TextChannelAction;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.TextChannelImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class DiscordServiceTests {

    private JDAImpl jda;
    private DiscordService service;

    @BeforeEach
    void initTest() {
        jda = Mockito.mock(JDAImpl.class);
        service = new DiscordService(jda);

        when(jda.getCacheFlags()).thenReturn(CacheFlag.getPrivileged());
    }

    @Test
    void getUserServersTest() {
        // GIVEN
        String userId = "123456";

        User user = Mockito.mock(User.class);

        List<Guild> guilds = List.of(
                new GuildImpl(jda, 1L).setName("Guild One"),
                new GuildImpl(jda, 2L).setName("Guild Two")
        );

        List<Server> expected = List.of(
                new Server("Guild One", null, 1L),
                new Server("Guild Two", null, 2L)
        );

        List<Server> result;

        when(jda.retrieveUserById(userId)).thenReturn(new CompletedRestAction<>(null, user));
        when(user.getMutualGuilds()).thenReturn(guilds);
        // WHEN
        result = service.getUserServers(userId);

        // THEN
        verify(jda, times(1)).retrieveUserById(userId);
        verify(user, times(1)).getMutualGuilds();
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void getNullUserServersTest() {
        // GIVEN
        String userId = "123456";

        List<Server> result;

        when(jda.retrieveUserById(userId)).thenReturn(new CompletedRestAction<>(null, null));
        // WHEN
        result = service.getUserServers(userId);

        // THEN
        verify(jda, times(1)).retrieveUserById(userId);
        assertThat(result).isEmpty();
    }

    @Test
    void getVisibleServerTextChannelsTest() {
        // GIVEN
        GuildImpl guild = Mockito.mock(GuildImpl.class);
        Member member = Mockito.mock(Member.class);

        List<net.dv8tion.jda.api.entities.TextChannel> channels = List.of(
                new TextChannelImpl(1L, guild).setName("Channel One"),
                new TextChannelImpl(2L, guild).setName("Channel Two")
        );

        List<TextChannel> expected = List.of(
                new TextChannel(1L, "Channel One"),
                new TextChannel(2L, "Channel Two")
        );

        List<TextChannel> result;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getTextChannels()).thenReturn(channels);
        when(member.hasAccess(any())).thenReturn(true);
        // WHEN
        result = service.getVisibleServerTextChannels(member);

        // THEN
        verify(member, times(1)).getGuild();
        verify(guild, times(1)).getTextChannels();
        verify(member, times(2)).hasAccess(any());
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void getVisibleServerTextChannelsWithNoChannelTest() {
        // GIVEN
        GuildImpl guild = Mockito.mock(GuildImpl.class);
        Member member = Mockito.mock(Member.class);

        List<TextChannel> result;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getTextChannels()).thenReturn(List.of());
        // WHEN
        result = service.getVisibleServerTextChannels(member);

        // THEN
        verify(member, times(1)).getGuild();
        verify(guild, times(1)).getTextChannels();
        assertThat(result).isEmpty();
    }

    @Test
    void filterInvisibleChannelsTest() {
        // GIVEN
        GuildImpl guild = Mockito.mock(GuildImpl.class);
        Member member = Mockito.mock(Member.class);

        TextChannelImpl visibleChannel = new TextChannelImpl(1L, guild).setName("Channel One");
        TextChannelImpl invisibleChannel = new TextChannelImpl(2L, guild).setName("Invisible Channel");

        List<net.dv8tion.jda.api.entities.TextChannel> channels = List.of(
                visibleChannel,
                invisibleChannel
        );

        List<TextChannel> expected = List.of(new TextChannel(1L, "Channel One"));

        List<TextChannel> result;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getTextChannels()).thenReturn(channels);
        when(member.hasAccess(invisibleChannel)).thenReturn(false);
        when(member.hasAccess(visibleChannel)).thenReturn(true);
        // WHEN
        result = service.getVisibleServerTextChannels(member);

        // THEN
        verify(member, times(1)).getGuild();
        verify(guild, times(1)).getTextChannels();
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void getPossibleActionsForChannelTest() {
        // GIVEN
        Member member = Mockito.mock(Member.class);
        net.dv8tion.jda.api.entities.TextChannel channel = Mockito.mock(net.dv8tion.jda.api.entities.TextChannel.class);

        List<TextChannelAction> expected = Arrays.stream(TextChannelAction.values())
                .filter(action -> !action.getId().equals(TextChannelAction.SET_TICKET_CHANNEL.getId()))
                .toList();

        List<TextChannelAction> result;

        when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(false);
        when(channel.canTalk(member)).thenReturn(true);
        // WHEN
        result = service.getPossibleActionsForChannel(member, channel);

        // THEN
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void sendMessageInChannelTest() {
        // GIVEN
        String message = "Hello world";

        Member member = Mockito.mock(Member.class);
        net.dv8tion.jda.api.entities.TextChannel channel = Mockito.mock(net.dv8tion.jda.api.entities.TextChannel.class);
        MessageAction action = Mockito.mock(MessageAction.class);

        when(channel.sendMessage(message)).thenReturn(action);
        // WHEN
        service.sendMessageInChannel(member, channel, message);

        // THEN
        verify(member, never()).hasPermission(any(Permission.class));
        verify(channel, times(1)).sendMessage(message);
        verify(action, times(1)).queue();
    }

    @Test
    void sendEveryoneMessageInChannelTest() {
        // GIVEN
        String message = "Hello @everyone !";

        Member member = Mockito.mock(Member.class);
        net.dv8tion.jda.api.entities.TextChannel channel = Mockito.mock(net.dv8tion.jda.api.entities.TextChannel.class);
        MessageAction action = Mockito.mock(MessageAction.class);

        when(channel.sendMessage(message)).thenReturn(action);
        when(member.hasPermission(Permission.MESSAGE_MENTION_EVERYONE)).thenReturn(true);
        // WHEN
        service.sendMessageInChannel(member, channel, message);

        // THEN
        verify(member, times(1)).hasPermission(Permission.MESSAGE_MENTION_EVERYONE);
        verify(channel, times(1)).sendMessage(message);
        verify(action, times(1)).queue();
    }

    @Test
    void refuseUnauthorizedEveryoneMessageInChannelTest() {
        // GIVEN
        String message = "Hello @everyone !";

        Member member = Mockito.mock(Member.class);
        net.dv8tion.jda.api.entities.TextChannel channel = Mockito.mock(net.dv8tion.jda.api.entities.TextChannel.class);
        MessageAction action = Mockito.mock(MessageAction.class);

        when(channel.sendMessage(message)).thenReturn(action);
        when(member.hasPermission(Permission.MESSAGE_MENTION_EVERYONE)).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.sendMessageInChannel(member, channel, message))
                .withMessage("You do not have the permission to mention everyone");

        // THEN
        verify(member, times(1)).hasPermission(Permission.MESSAGE_MENTION_EVERYONE);
        verify(channel, never()).sendMessage(any(String.class));
        verify(action, never()).queue();
    }

    @Test
    void refuseUnauthorizedHereMessageInChannelTest() {
        // GIVEN
        String message = "Hello @here !";

        Member member = Mockito.mock(Member.class);
        net.dv8tion.jda.api.entities.TextChannel channel = Mockito.mock(net.dv8tion.jda.api.entities.TextChannel.class);
        MessageAction action = Mockito.mock(MessageAction.class);

        when(channel.sendMessage(message)).thenReturn(action);
        when(member.hasPermission(Permission.MESSAGE_MENTION_EVERYONE)).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.sendMessageInChannel(member, channel, message))
                .withMessage("You do not have the permission to mention everyone");

        // THEN
        verify(member, times(1)).hasPermission(Permission.MESSAGE_MENTION_EVERYONE);
        verify(channel, never()).sendMessage(any(String.class));
        verify(action, never()).queue();
    }

}

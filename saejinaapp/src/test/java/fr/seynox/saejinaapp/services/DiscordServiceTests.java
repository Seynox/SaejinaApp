package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.TextChannelImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

        when(jda.retrieveUserById(userId)).thenReturn(new CompletedRestAction<>(jda, user));
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

        when(jda.retrieveUserById(userId)).thenReturn(new CompletedRestAction<>(jda, null));
        // WHEN
        result = service.getUserServers(userId);

        // THEN
        verify(jda, times(1)).retrieveUserById(userId);
        assertThat(result).isEmpty();
    }

    @Test
    void isUserInServerTest() {
        // GIVEN
        String userId = "123456";
        long serverId = 456123;

        Guild guild = Mockito.mock(Guild.class);
        Member member = Mockito.mock(Member.class);

        boolean result;

        when(jda.getGuildById(serverId)).thenReturn(guild);
        when(guild.retrieveMemberById(userId)).thenReturn(new CompletedRestAction<>(jda, member));
        // WHEN
        result = service.isUserInServer(userId, serverId);

        // THEN
        verify(jda, times(1)).getGuildById(serverId);
        verify(guild, times(1)).retrieveMemberById(userId);

        assertThat(result).isTrue();
    }

    @Test
    void isUserInNullServerTest() {
        // GIVEN
        String userId = "123456";
        long serverId = 456123;

        boolean result;

        when(jda.getGuildById(serverId)).thenReturn(null);
        // WHEN
        result = service.isUserInServer(userId, serverId);

        // THEN
        verify(jda, times(1)).getGuildById(serverId);

        assertThat(result).isFalse();
    }

    @Test
    void isNullUserInServerTest() {
        // GIVEN
        String userId = "123456";
        long serverId = 456123;

        Guild guild = Mockito.mock(Guild.class);

        boolean result;

        when(jda.getGuildById(serverId)).thenReturn(guild);
        when(guild.retrieveMemberById(userId)).thenReturn(new CompletedRestAction<>(jda, null));
        // WHEN
        result = service.isUserInServer(userId, serverId);

        // THEN
        verify(jda, times(1)).getGuildById(serverId);
        verify(guild, times(1)).retrieveMemberById(userId);

        assertThat(result).isFalse();
    }

    @Test
    void getServerTextChannelsTest() {
        // GIVEN
        long serverId = 123456;

        GuildImpl guild = Mockito.mock(GuildImpl.class);

        List<net.dv8tion.jda.api.entities.TextChannel> channels = List.of(
                new TextChannelImpl(1L, guild).setName("Channel One"),
                new TextChannelImpl(2L, guild).setName("Channel Two")
        );

        List<TextChannel> expected = List.of(
                new TextChannel(1L, "Channel One"),
                new TextChannel(2L, "Channel Two")
        );

        List<TextChannel> result;

        when(jda.getGuildById(serverId)).thenReturn(guild);
        when(guild.getTextChannels()).thenReturn(channels);
        // WHEN
        result = service.getServerTextChannels(serverId);

        // THEN
        verify(jda, times(1)).getGuildById(serverId);
        verify(guild, times(1)).getTextChannels();
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void getNullServerTextChannelsTest() {
        // GIVEN
        long serverId = 123456;

        List<TextChannel> result;

        when(jda.getGuildById(serverId)).thenReturn(null);
        // WHEN
        result = service.getServerTextChannels(serverId);

        // THEN
        verify(jda, times(1)).getGuildById(serverId);
        assertThat(result).isEmpty();
    }

    @Test
    void getServerTextChannelsWithNoChannelTest() {
        // GIVEN
        long serverId = 123456;

        GuildImpl guild = Mockito.mock(GuildImpl.class);

        List<TextChannel> result;

        when(jda.getGuildById(serverId)).thenReturn(guild);
        when(guild.getTextChannels()).thenReturn(List.of());
        // WHEN
        result = service.getServerTextChannels(serverId);

        // THEN
        verify(jda, times(1)).getGuildById(serverId);
        verify(guild, times(1)).getTextChannels();
        assertThat(result).isEmpty();
    }

}

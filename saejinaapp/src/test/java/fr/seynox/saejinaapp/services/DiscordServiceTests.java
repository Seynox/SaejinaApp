package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.models.Selectable;
import fr.seynox.saejinaapp.models.SelectableImpl;
import fr.seynox.saejinaapp.models.TextChannelAction;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class DiscordServiceTests {

    private JDAImpl jda;
    private DiscordService service;

    private Member member;
    private TextChannel channel;

    @BeforeEach
    void initTest() {
        jda = Mockito.mock(JDAImpl.class);
        service = new DiscordService(jda);

        member = Mockito.mock(Member.class);
        channel = Mockito.mock(TextChannel.class);

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

        List<Selectable> expected = List.of(
                new SelectableImpl(1L, null, "Guild One"),
                new SelectableImpl(2L, null, "Guild Two")
        );

        List<Selectable> result;

        when(jda.retrieveUserById(userId)).thenReturn(new CompletedRestAction<>(null, user));
        when(user.getMutualGuilds()).thenReturn(guilds);
        // WHEN
        result = service.getUserServers(userId);

        // THEN
        verify(jda).retrieveUserById(userId);
        verify(user).getMutualGuilds();
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void getNullUserServersTest() {
        // GIVEN
        String userId = "123456";

        List<Selectable> result;

        when(jda.retrieveUserById(userId)).thenReturn(new CompletedRestAction<>(null, null));
        // WHEN
        result = service.getUserServers(userId);

        // THEN
        verify(jda).retrieveUserById(userId);
        assertThat(result).isEmpty();
    }

    @Test
    void getPossibleActionsForChannelTest() {
        // GIVEN

        List<TextChannelAction> expected = List.of(TextChannelAction.SEND_MESSAGE);

        List<TextChannelAction> result;

        when(member.hasPermission(Permission.MANAGE_CHANNEL)).thenReturn(false);
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

        MessageAction action = Mockito.mock(MessageAction.class);

        when(channel.sendMessage(message)).thenReturn(action);
        // WHEN
        service.sendMessageInChannel(member, channel, message);

        // THEN
        verify(member, never()).hasPermission(any(Permission.class));
        verify(channel).sendMessage(message);
        verify(action).queue();
    }

    @Test
    void sendEveryoneMessageInChannelTest() {
        // GIVEN
        String message = "Hello @everyone !";

        MessageAction action = Mockito.mock(MessageAction.class);

        when(channel.sendMessage(message)).thenReturn(action);
        when(member.hasPermission(Permission.MESSAGE_MENTION_EVERYONE)).thenReturn(true);
        // WHEN
        service.sendMessageInChannel(member, channel, message);

        // THEN
        verify(member).hasPermission(Permission.MESSAGE_MENTION_EVERYONE);
        verify(channel).sendMessage(message);
        verify(action).queue();
    }

    @Test
    void refuseUnauthorizedEveryoneMessageInChannelTest() {
        // GIVEN
        String message = "Hello @everyone !";

        MessageAction action = Mockito.mock(MessageAction.class);

        when(channel.sendMessage(message)).thenReturn(action);
        when(member.hasPermission(Permission.MESSAGE_MENTION_EVERYONE)).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.sendMessageInChannel(member, channel, message))
                .withMessage("You do not have the permission to mention everyone");

        // THEN
        verify(member).hasPermission(Permission.MESSAGE_MENTION_EVERYONE);
        verify(channel, never()).sendMessage(any(String.class));
        verify(action, never()).queue();
    }

    @Test
    void refuseUnauthorizedHereMessageInChannelTest() {
        // GIVEN
        String message = "Hello @here !";

        MessageAction action = Mockito.mock(MessageAction.class);

        when(channel.sendMessage(message)).thenReturn(action);
        when(member.hasPermission(Permission.MESSAGE_MENTION_EVERYONE)).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.sendMessageInChannel(member, channel, message))
                .withMessage("You do not have the permission to mention everyone");

        // THEN
        verify(member).hasPermission(Permission.MESSAGE_MENTION_EVERYONE);
        verify(channel, never()).sendMessage(any(String.class));
        verify(action, never()).queue();
    }

    @Test
    void getMentionableRolesTest() {
        // GIVEN
        String roleName = "My role !";
        String roleMention = "<@&123456789>";

        Guild guild = Mockito.mock(Guild.class);

        Role role = Mockito.mock(Role.class);
        Role nonMentionableRole = Mockito.mock(Role.class);
        List<Role> roles = List.of(role, nonMentionableRole);

        List<Selectable> expected = List.of(
                new SelectableImpl(roleMention, roleName)
        );

        List<Selectable> result;

        when(nonMentionableRole.isMentionable()).thenReturn(false);
        when(role.isMentionable()).thenReturn(true);

        when(role.getName()).thenReturn(roleName);
        when(role.getAsMention()).thenReturn(roleMention);

        when(guild.getRoles()).thenReturn(roles);
        // WHEN
        result = service.getMentionableRoles(guild);

        // THEN
        assertThat(result).containsExactlyElementsOf(expected);
        verify(guild).getRoles();
    }

    @Test
    void getMentionableUsersTest() {
        // GIVEN
        String username = "Bob1234";
        String userMention = "<@123456789>";

        Guild guild = Mockito.mock(Guild.class);

        Member member = Mockito.mock(Member.class);
        List<Member> members = List.of(member);

        List<Selectable> expected = List.of(
                new SelectableImpl(userMention, username)
        );

        List<Selectable> result;

        when(member.getEffectiveName()).thenReturn(username);
        when(member.getAsMention()).thenReturn(userMention);

        when(guild.getMembers()).thenReturn(members);
        // WHEN
        result = service.getMentionableUsers(guild);

        // THEN
        assertThat(result).containsExactlyElementsOf(expected);
        verify(guild).getMembers();
    }

}

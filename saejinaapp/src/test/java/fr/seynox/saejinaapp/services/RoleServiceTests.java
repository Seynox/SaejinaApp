package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.models.Selectable;
import fr.seynox.saejinaapp.models.SelectableImpl;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class RoleServiceTests {

    private RoleService service;

    private Member member;
    private Guild guild;
    private Role role;

    @BeforeEach
    void initTest() {
        service = new RoleService();

        member = Mockito.mock(Member.class);
        guild = Mockito.mock(Guild.class);
        role = Mockito.mock(Role.class);
    }

    @Test
    void getAssignableRolesForMemberTest() {
        // GIVEN
        long roleId = 123456789;
        String roleName = "My role";

        Role nonAssignableRole = Mockito.mock(Role.class);
        List<Role> roles = List.of(role, nonAssignableRole);

        List<Selectable> expectedRoles = List.of(new SelectableImpl(roleId, roleName));
        List<Selectable> result;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getRoles()).thenReturn(roles);
        when(member.canInteract(nonAssignableRole)).thenReturn(false);
        when(member.canInteract(role)).thenReturn(true);
        when(role.getName()).thenReturn(roleName);
        when(role.getIdLong()).thenReturn(roleId);
        // WHEN
        result = service.getAssignableRolesForMember(member);

        // THEN
        assertThat(result).containsExactlyElementsOf(expectedRoles);
        verify(member, times(2)).canInteract(any(Role.class));
    }

    @Test
    void getEmptyAssignableRolesForMemberTest() {
        // GIVEN
        Role nonAssignableRole = Mockito.mock(Role.class);
        List<Role> roles = List.of(role, nonAssignableRole);

        List<Selectable> result;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getRoles()).thenReturn(roles);
        when(member.canInteract(any(Role.class))).thenReturn(false);
        // WHEN
        result = service.getAssignableRolesForMember(member);

        // THEN
        assertThat(result).isEmpty();
        verify(member, times(2)).canInteract(any(Role.class));
    }

    @Test
    void filterPublicRoleTest() {
        // GIVEN
        long roleId = 123456789;
        String roleName = "My role";

        Role publicRole = Mockito.mock(Role.class);
        List<Role> roles = List.of(role, publicRole);

        List<Selectable> expectedRoles = List.of(new SelectableImpl(roleId, roleName));
        List<Selectable> result;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getRoles()).thenReturn(roles);
        when(member.canInteract(publicRole)).thenReturn(true);
        when(role.isPublicRole()).thenReturn(false);
        when(publicRole.isPublicRole()).thenReturn(true);
        when(member.canInteract(role)).thenReturn(true);
        when(role.getName()).thenReturn(roleName);
        when(role.getIdLong()).thenReturn(roleId);
        // WHEN
        result = service.getAssignableRolesForMember(member);

        // THEN
        assertThat(result).containsExactlyElementsOf(expectedRoles);
        verify(member, times(2)).canInteract(any(Role.class));
    }

    @Test
    void sendRoleButtonInChannelTest() {
        // GIVEN
        TextChannel channel = Mockito.mock(TextChannel.class);

        long roleId = 123456789;
        String roleName = "My role";

        String expectedButtonId = "role-assign#123456789";

        MessageAction action = Mockito.mock(MessageAction.class);
        ArgumentCaptor<Button> captor = ArgumentCaptor.forClass(Button.class);

        when(channel.canTalk(any(Member.class))).thenReturn(true);
        when(member.hasPermission(any(Permission.class))).thenReturn(true);

        when(role.getIdLong()).thenReturn(roleId);
        when(role.getName()).thenReturn(roleName);
        when(channel.sendMessage(anyString())).thenReturn(action);
        when(action.setActionRow(any(Button.class))).thenReturn(action);
        // WHEN
        service.sendRoleButtonInChannel(member, channel, role);

        // THEN
        verify(channel).canTalk(member);
        verify(member).hasPermission(Permission.MANAGE_CHANNEL);
        verify(action).setActionRow(captor.capture());
        verify(action).queue();

        Button button = captor.getValue();
        assertThat(button.getId()).isEqualTo(expectedButtonId);
        assertThat(button.getLabel()).isEqualTo(roleName);
    }

    @Test
    void refuseToSendButtonInUnauthorizedChannelTest() {
        // GIVEN
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(channel.canTalk(any(Member.class))).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.sendRoleButtonInChannel(member, channel, role));

        // THEN
        verify(channel).canTalk(member);
    }

    @Test
    void refuseUnauthorizedButtonSendInChannelTest() {
        // GIVEN
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(channel.canTalk(any(Member.class))).thenReturn(true);
        when(member.hasPermission(any(Permission.class))).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.sendRoleButtonInChannel(member, channel, role));

        // THEN
        verify(channel).canTalk(member);
        verify(member).hasPermission(Permission.MANAGE_CHANNEL);
    }

}

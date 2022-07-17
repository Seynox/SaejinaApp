package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.DiscordInteractionException;
import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.models.Selectable;
import fr.seynox.saejinaapp.models.SelectableImpl;
import fr.seynox.saejinaapp.utils.ButtonUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
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
    private ButtonUtils buttonUtils;

    private Member member;
    private Guild guild;
    private Role role;

    @BeforeEach
    void initTest() {
        buttonUtils = Mockito.mock(ButtonUtils.class);
        service = new RoleService(buttonUtils);

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

        ArgumentCaptor<Button> captor = ArgumentCaptor.forClass(Button.class);

        when(channel.canTalk(any(Member.class))).thenReturn(true);
        when(member.hasPermission(any(Permission.class))).thenReturn(true);

        when(role.getIdLong()).thenReturn(roleId);
        when(role.getName()).thenReturn(roleName);
        // WHEN
        service.sendRoleButtonInChannel(member, channel, role);

        // THEN
        verify(channel).canTalk(member);
        verify(member).hasPermission(Permission.MANAGE_CHANNEL);
        verify(buttonUtils).sendOrAppendButton(eq(channel), captor.capture());

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
        verify(buttonUtils, never()).sendOrAppendButton(any(), any());
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
        verify(buttonUtils, never()).sendOrAppendButton(any(), any());
    }

    @Test
    void addRoleToMemberTest() throws DiscordInteractionException {
        // GIVEN
        long roleId = 123456789;
        long placeholderRoleId = 987654321;
        Role placeholderRole = Mockito.mock(Role.class);

        AuditableRestAction<Void> action = Mockito.mock(AuditableRestAction.class);

        List<Role> currentRoles = List.of(placeholderRole);

        boolean wasRoleAdded;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getRoleById(anyLong())).thenReturn(role);
        when(member.getRoles()).thenReturn(currentRoles);
        when(placeholderRole.getIdLong()).thenReturn(placeholderRoleId);
        when(guild.addRoleToMember(any(Member.class), any(Role.class))).thenReturn(action);
        // WHEN
        wasRoleAdded = service.toggleRoleForMember(member, roleId);

        // THEN
        verify(guild).getRoleById(roleId);
        verify(guild).addRoleToMember(member, role);
        verify(guild, never()).removeRoleFromMember(any(), any());
        assertThat(wasRoleAdded).isTrue();
    }

    @Test
    void removeRoleFromMemberTest() throws DiscordInteractionException {
        // GIVEN
        long roleId = 123456789;

        AuditableRestAction<Void> action = Mockito.mock(AuditableRestAction.class);

        List<Role> currentRoles = List.of(role);

        boolean wasRoleAdded;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getRoleById(anyLong())).thenReturn(role);
        when(member.getRoles()).thenReturn(currentRoles);
        when(role.getIdLong()).thenReturn(roleId);
        when(guild.removeRoleFromMember(any(Member.class), any(Role.class))).thenReturn(action);
        // WHEN
        wasRoleAdded = service.toggleRoleForMember(member, roleId);

        // THEN
        verify(guild).getRoleById(roleId);
        verify(guild, never()).addRoleToMember(any(), any());
        verify(guild).removeRoleFromMember(member, role);
        assertThat(wasRoleAdded).isFalse();
    }

    @Test
    void refuseToToggleRoleFromNullMemberTest() {
        // GIVEN
        long roleId = 123456789;

        // WHEN
        assertThatExceptionOfType(DiscordInteractionException.class) // THEN
                .isThrownBy(() -> service.toggleRoleForMember(null, roleId));
    }

    @Test
    void refuseToToggleNullRoleFromMemberTest() {
        // GIVEN
        long roleId = 123456789;

        when(member.getGuild()).thenReturn(guild);
        when(guild.getRoleById(anyLong())).thenReturn(null);
        // WHEN
        assertThatExceptionOfType(DiscordInteractionException.class)
                .isThrownBy(() -> service.toggleRoleForMember(member, roleId));

        // THEN
        verify(guild).getRoleById(roleId);
    }

}

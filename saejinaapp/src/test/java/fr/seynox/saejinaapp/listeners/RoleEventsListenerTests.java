package fr.seynox.saejinaapp.listeners;

import fr.seynox.saejinaapp.exceptions.DiscordInteractionException;
import fr.seynox.saejinaapp.services.RoleService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


class RoleEventsListenerTests {

    private RoleEventsListener listener;
    private RoleService service;

    private long roleId;
    private Member member;
    private IReplyCallback replyCallback;
    private ReplyCallbackAction action;

    @BeforeEach
    void initTest() {
        service = Mockito.mock(RoleService.class);
        listener = new RoleEventsListener(service);

        roleId = 123456789;
        member = Mockito.mock(Member.class);
        replyCallback = Mockito.mock(IReplyCallback.class);
        action = Mockito.mock(ReplyCallbackAction.class);
    }

    @Test
    void onButtonPressTest() throws DiscordInteractionException {
        // GIVEN
        String buttonId = "role-assign#%s".formatted(roleId);
        String expectedMessage = "Successfully added the role";

        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);

        when(event.getComponentId()).thenReturn(buttonId);
        when(replyCallback.getMember()).thenReturn(member);
        when(service.toggleRoleForMember(any(Member.class), anyLong())).thenReturn(true);
        when(replyCallback.reply(anyString())).thenReturn(action);
        when(action.setEphemeral(anyBoolean())).thenReturn(action);
        // WHEN
        listener.toggleRole(roleId, replyCallback);

        // THEN
        verify(service).toggleRoleForMember(member, roleId);
        verify(replyCallback).reply(expectedMessage);
        verify(action).setEphemeral(true);
    }

    @Test
    void onOtherButtonPressTest() throws DiscordInteractionException {
        // GIVEN
        String buttonId = "ticket-invite#123456789";

        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);

        when(event.getComponentId()).thenReturn(buttonId);
        // WHEN
        listener.onButtonPress(event);

        // THEN
        verify(service, never()).toggleRoleForMember(any(), any());
    }

    @Test
    void addRoleTest() throws DiscordInteractionException {
        // GIVEN
        String expectedMessage = "Successfully added the role";

        when(replyCallback.getMember()).thenReturn(member);
        when(service.toggleRoleForMember(any(Member.class), anyLong())).thenReturn(true);
        when(replyCallback.reply(anyString())).thenReturn(action);
        when(action.setEphemeral(anyBoolean())).thenReturn(action);
        // WHEN
        listener.toggleRole(roleId, replyCallback);

        // THEN
        verify(service).toggleRoleForMember(member, roleId);
        verify(replyCallback).reply(expectedMessage);
        verify(action).setEphemeral(true);
    }

    @Test
    void removeRoleTest() throws DiscordInteractionException {
        // GIVEN
        String expectedMessage = "Successfully removed the role";

        when(replyCallback.getMember()).thenReturn(member);
        when(service.toggleRoleForMember(any(Member.class), anyLong())).thenReturn(false);
        when(replyCallback.reply(anyString())).thenReturn(action);
        when(action.setEphemeral(anyBoolean())).thenReturn(action);
        // WHEN
        listener.toggleRole(roleId, replyCallback);

        // THEN
        verify(service).toggleRoleForMember(member, roleId);
        verify(replyCallback).reply(expectedMessage);
        verify(action).setEphemeral(true);
    }

    @Test
    void toggleNullMemberRoleTest() throws DiscordInteractionException {
        // GIVEN
        String errorMessage = "Error ! Member cannot be null";

        when(replyCallback.getMember()).thenReturn(null);
        when(service.toggleRoleForMember(eq(null), anyLong()))
                .thenThrow(new DiscordInteractionException(errorMessage));
        when(replyCallback.reply(anyString())).thenReturn(action);
        when(action.setEphemeral(anyBoolean())).thenReturn(action);
        // WHEN
        listener.toggleRole(roleId, replyCallback);

        // THEN
        verify(service).toggleRoleForMember(null, roleId);
        verify(replyCallback).reply(errorMessage);
        verify(action).setEphemeral(true);
    }

}

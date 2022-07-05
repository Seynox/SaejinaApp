package fr.seynox.saejinaapp.listeners;

import fr.seynox.saejinaapp.exceptions.DiscordInteractionException;
import fr.seynox.saejinaapp.services.TicketService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static fr.seynox.saejinaapp.services.TicketService.*;
import static org.mockito.Mockito.*;

class TicketEventsListenerTests {

    private TicketEventsListener listener;
    private TicketService service;

    private Button button;

    @BeforeEach
    void initTest() {
        service = Mockito.mock(TicketService.class);
        listener = new TicketEventsListener(service);

        button = Mockito.mock(Button.class);
    }

    @Test
    void onCreationButtonPressTest() throws DiscordInteractionException {
        // GIVEN
        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);
        Member member = Mockito.mock(Member.class);
        Modal modal = Mockito.mock(Modal.class);
        ModalCallbackAction action = Mockito.mock(ModalCallbackAction.class);

        when(event.getButton()).thenReturn(button);
        when(event.getComponentId()).thenReturn(TICKET_CREATION_ID);
        when(event.getMember()).thenReturn(member);
        when(service.getTicketCreationForm(any(Member.class), any(Button.class))).thenReturn(modal);
        when(event.replyModal(any(Modal.class))).thenReturn(action);
        // WHEN
        listener.onButtonPress(event);

        // THEN
        verify(service).getTicketCreationForm(member, button);
        verify(event).replyModal(modal);
        verify(action).queue();
    }

    @Test
    void onCloseButtonPressTest() {
        // GIVEN
        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);
        RestAction<Void> action = Mockito.mock(RestAction.class);

        Button confirmationButton = Button.of(ButtonStyle.DANGER, TICKET_CLOSE_CONFIRM_ID, "Are you sure ?");

        when(event.getButton()).thenReturn(button);
        when(event.getComponentId()).thenReturn(TICKET_CLOSE_ID);
        when(event.editButton(confirmationButton)).thenReturn(action);
        // WHEN
        listener.onButtonPress(event);

        // THEN
        verify(event).editButton(confirmationButton);
        verify(action).queue();
    }

    @Test
    void onCloseConfirmButtonPressTest() {
        // GIVEN
        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);
        ReplyCallbackAction action = Mockito.mock(ReplyCallbackAction.class);

        String message = "Closing the ticket ! Deleting channel in 30sec...";

        when(event.getButton()).thenReturn(button);
        when(event.getComponentId()).thenReturn(TICKET_CLOSE_CONFIRM_ID);
        when(event.reply(message)).thenReturn(action);
        // WHEN
        listener.onButtonPress(event);

        // THEN
        verify(event).reply(message);
        verify(action).queue();
    }

    @Test
    void onInviteButtonPressTest() throws DiscordInteractionException {
        // GIVEN
        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);
        RestAction<Void> action = Mockito.mock(RestAction.class);
        ReplyCallbackAction replyCallback = Mockito.mock(ReplyCallbackAction.class);

        String userId = "123456789";
        String successMessage = "The ticket owner (<@%s>) has been invited to the channel !"
                .formatted(userId);
        String buttonId = TICKET_INVITE_ID_TEMPLATE.formatted(userId);
        TextChannel channel = Mockito.mock(TextChannel.class);
        Button disabledButton = Mockito.mock(Button.class);

        when(event.getComponentId()).thenReturn(buttonId);
        when(event.getButton()).thenReturn(button);
        when(event.getTextChannel()).thenReturn(channel);
        when(event.reply(anyString())).thenReturn(replyCallback);
        when(event.editButton(disabledButton)).thenReturn(action);
        when(button.asDisabled()).thenReturn(disabledButton);
        // WHEN
        listener.onButtonPress(event);

        // THEN
        verify(service).inviteUserToTicketChannel(userId, channel);
        verify(event).reply(successMessage);
        verify(replyCallback).queue();
        verify(event).editButton(disabledButton);
        verify(action).queue();
    }

    @Test
    void showTicketCreationFormTest() throws DiscordInteractionException {
        // GIVEN
        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);
        Member member = Mockito.mock(Member.class);
        Modal modal = Mockito.mock(Modal.class);
        ModalCallbackAction action = Mockito.mock(ModalCallbackAction.class);

        when(event.getMember()).thenReturn(member);
        when(event.getButton()).thenReturn(button);
        when(service.getTicketCreationForm(any(Member.class), any(Button.class))).thenReturn(modal);
        when(event.replyModal(any(Modal.class))).thenReturn(action);
        // WHEN
        listener.showTicketCreationForm(event);

        // THEN
        verify(service).getTicketCreationForm(member, button);
        verify(event).replyModal(modal);
        verify(action).queue();
    }

    @Test
    void refuseToShowTicketCreationFormToNullMemberTest() throws DiscordInteractionException {
        // GIVEN
        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);
        Member member = Mockito.mock(Member.class);
        ReplyCallbackAction action = Mockito.mock(ReplyCallbackAction.class);

        String errorMessage = "Member cannot be null !";

        when(event.getMember()).thenReturn(member);
        when(event.getButton()).thenReturn(button);
        when(service.getTicketCreationForm(any(Member.class), any(Button.class)))
                .thenThrow(new DiscordInteractionException(errorMessage));
        when(event.reply(anyString())).thenReturn(action);
        when(action.setEphemeral(anyBoolean())).thenReturn(action);
        // WHEN
        listener.showTicketCreationForm(event);

        // THEN
        verify(service).getTicketCreationForm(member, button);
        verify(event).reply(errorMessage);
        verify(action).setEphemeral(true);
        verify(action).queue();
    }

    @Test
    void inviteMemberToChannelTest() throws DiscordInteractionException {
        // GIVEN
        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);
        RestAction<Void> action = Mockito.mock(RestAction.class);
        ReplyCallbackAction replyCallback = Mockito.mock(ReplyCallbackAction.class);

        String userId = "123456789";
        String successMessage = "The ticket owner (<@%s>) has been invited to the channel !"
                .formatted(userId);
        TextChannel channel = Mockito.mock(TextChannel.class);
        Button disabledButton = Mockito.mock(Button.class);

        when(event.getButton()).thenReturn(button);
        when(event.getTextChannel()).thenReturn(channel);
        when(event.reply(anyString())).thenReturn(replyCallback);
        when(event.editButton(disabledButton)).thenReturn(action);
        when(button.asDisabled()).thenReturn(disabledButton);
        // WHEN
        listener.inviteMemberToChannel(userId, event);

        // THEN
        verify(service).inviteUserToTicketChannel(userId, channel);
        verify(event).reply(successMessage);
        verify(replyCallback).queue();
        verify(event).editButton(disabledButton);
        verify(action).queue();
    }

    @Test
    void refuseToInviteNullMemberToChannelTest() throws DiscordInteractionException {
        // GIVEN
        ButtonInteractionEvent event = Mockito.mock(ButtonInteractionEvent.class);
        ReplyCallbackAction replyCallback = Mockito.mock(ReplyCallbackAction.class);

        String userId = "123456789";
        String errorMessage = "Member cannot be found !";
        TextChannel channel = Mockito.mock(TextChannel.class);

        when(event.getButton()).thenReturn(button);
        when(event.getTextChannel()).thenReturn(channel);
        doThrow(new DiscordInteractionException(errorMessage)).when(service).inviteUserToTicketChannel(anyString(), any(TextChannel.class));
        when(event.reply(anyString())).thenReturn(replyCallback);
        when(replyCallback.setEphemeral(anyBoolean())).thenReturn(replyCallback);
        // WHEN
        listener.inviteMemberToChannel(userId, event);

        // THEN
        verify(service).inviteUserToTicketChannel(userId, channel);
        verify(event).reply(errorMessage);
        verify(replyCallback).setEphemeral(true);
        verify(replyCallback).queue();
        verify(event, never()).editButton(any());
    }

    @Test
    void submitTicketToServerTest() {
        // GIVEN
        ModalInteractionEvent event = Mockito.mock(ModalInteractionEvent.class);
        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);
        TextChannel channel = Mockito.mock(TextChannel.class);
        ReplyCallbackAction action = Mockito.mock(ReplyCallbackAction.class);

        String successMessage = "Your ticket was submitted !";

        when(event.getMember()).thenReturn(member);
        when(member.getGuild()).thenReturn(guild);
        when(service.createTicketChannel(guild)).thenReturn(channel);
        when(event.reply(anyString())).thenReturn(action);
        when(action.setEphemeral(anyBoolean())).thenReturn(action);
        // WHEN
        listener.submitTicketToServer(event);

        // THEN
        verify(service).sendTicketToChannel(member, channel, event);
        verify(event).reply(successMessage);
        verify(action).setEphemeral(true);
        verify(action).queue();
    }

    @Test
    void refuseTicketSubmitInDMTest() {
        // GIVEN
        ModalInteractionEvent event = Mockito.mock(ModalInteractionEvent.class);
        ReplyCallbackAction action = Mockito.mock(ReplyCallbackAction.class);

        String expectedErrorMessage = "Error ! Tickets can only be submitted in servers";

        when(event.getMember()).thenReturn(null);
        when(event.reply(expectedErrorMessage)).thenReturn(action);
        when(action.setEphemeral(true)).thenReturn(action);
        // WHEN
        listener.submitTicketToServer(event);

        // THEN
        verify(service, never()).sendTicketToChannel(any(), any(), any());
        verify(event).reply(expectedErrorMessage);
        verify(action).setEphemeral(true);
        verify(action).queue();
    }

    @Test
    void refuseTicketSubmitWithoutPermissionsTest() {
        // GIVEN
        ModalInteractionEvent event = Mockito.mock(ModalInteractionEvent.class);
        ReplyCallbackAction action = Mockito.mock(ReplyCallbackAction.class);

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);

        String expectedErrorMessage = "Error ! I do not have the permissions required to create channels. Please contact a server administrator";

        when(event.getMember()).thenReturn(member);
        when(member.getGuild()).thenReturn(guild);
        when(service.createTicketChannel(guild)).thenThrow(InsufficientPermissionException.class);
        when(event.reply(expectedErrorMessage)).thenReturn(action);
        when(action.setEphemeral(true)).thenReturn(action);
        // WHEN
        listener.submitTicketToServer(event);

        // THEN
        verify(service, never()).sendTicketToChannel(any(), any(), any());
        verify(event).reply(expectedErrorMessage);
        verify(action).setEphemeral(true);
        verify(action).queue();
    }

    @Test
    void refuseInvalidTicketSubmitTest() {
        // GIVEN
        ModalInteractionEvent event = Mockito.mock(ModalInteractionEvent.class);
        ReplyCallbackAction action = Mockito.mock(ReplyCallbackAction.class);

        Member member = Mockito.mock(Member.class);
        Guild guild = Mockito.mock(Guild.class);
        TextChannel channel = Mockito.mock(TextChannel.class);

        String expectedErrorMessage = "Error ! The ticket you submitted is invalid";

        when(event.getMember()).thenReturn(member);
        when(member.getGuild()).thenReturn(guild);
        when(service.createTicketChannel(guild)).thenReturn(channel);
        doThrow(NullPointerException.class).when(service).sendTicketToChannel(member, channel, event);
        when(event.reply(expectedErrorMessage)).thenReturn(action);
        when(action.setEphemeral(true)).thenReturn(action);
        // WHEN
        listener.submitTicketToServer(event);

        // THEN
        verify(service).sendTicketToChannel(member, channel, event);
        verify(event).reply(expectedErrorMessage);
        verify(action).setEphemeral(true);
        verify(action).queue();
    }


}

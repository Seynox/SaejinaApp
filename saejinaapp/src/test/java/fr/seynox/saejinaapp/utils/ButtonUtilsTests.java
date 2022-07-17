package fr.seynox.saejinaapp.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class ButtonUtilsTests {

    private ButtonUtils utils;

    private TextChannel channel;
    private Button button;
    private Message message;
    private MessageAction action;

    @BeforeEach
    void initTest() {
        utils = new ButtonUtils();

        channel = Mockito.mock(TextChannel.class);
        button = Mockito.mock(Button.class);
        message = Mockito.mock(Message.class);
        action = Mockito.mock(MessageAction.class);

        when(button.getType()).thenReturn(Component.Type.BUTTON);
    }

    @Test
    void sendButtonWhenMessageIsFromOtherUserTest() {
        // GIVEN
        String messageContent = "** **";
        long messageId = 123456789;
        long authorId = 987654321;
        long selfId = 789123456;

        ActionRow expected = ActionRow.of(button);

        SelfUser selfUser = Mockito.mock(SelfUser.class);
        JDA jda = Mockito.mock(JDA.class);
        User author = Mockito.mock(User.class);
        RestAction<Message> latestMessageAction = Mockito.mock(RestAction.class);

        when(channel.getLatestMessageIdLong()).thenReturn(messageId);
        when(channel.retrieveMessageById(anyLong())).thenReturn(latestMessageAction);
        when(latestMessageAction.complete()).thenReturn(message);

        when(message.getAuthor()).thenReturn(author);
        when(author.getIdLong()).thenReturn(authorId);

        when(channel.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(selfUser);
        when(selfUser.getIdLong()).thenReturn(selfId);

        when(channel.sendMessage(anyString())).thenReturn(action);
        when(action.setActionRows(any(ActionRow.class))).thenReturn(action);
        // WHEN
        utils.sendOrAppendButton(channel, button);

        // THEN
        verify(channel).retrieveMessageById(messageId);
        verify(message, never()).editMessageComponents(anyCollection());
        verify(channel).sendMessage(messageContent);
        verify(action).setActionRows(expected);
        verify(action).queue();
    }

    @Test
    void sendButtonWhenMessageHasSameIdButtonTest() {
        // GIVEN
        String messageContent = "** **";
        String buttonId = "my-button";
        long messageId = 123456789;
        long authorId = 987654321;

        ActionRow expected = ActionRow.of(button);

        SelfUser selfUser = Mockito.mock(SelfUser.class);
        JDA jda = Mockito.mock(JDA.class);
        User author = Mockito.mock(User.class);
        RestAction<Message> latestMessageAction = Mockito.mock(RestAction.class);

        when(channel.getLatestMessageIdLong()).thenReturn(messageId);
        when(channel.retrieveMessageById(anyLong())).thenReturn(latestMessageAction);
        when(latestMessageAction.complete()).thenReturn(message);

        when(message.getAuthor()).thenReturn(author);
        when(author.getIdLong()).thenReturn(authorId);

        when(channel.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(selfUser);
        when(selfUser.getIdLong()).thenReturn(authorId);

        when(button.getId()).thenReturn(buttonId);
        when(message.getButtonById(anyString())).thenReturn(button);

        when(channel.sendMessage(anyString())).thenReturn(action);
        when(action.setActionRows(any(ActionRow.class))).thenReturn(action);
        // WHEN
        utils.sendOrAppendButton(channel, button);

        // THEN
        verify(channel).retrieveMessageById(messageId);
        verify(message).getButtonById(buttonId);
        verify(message, never()).editMessageComponents(anyCollection());
        verify(channel).sendMessage(messageContent);
        verify(action).setActionRows(expected);
        verify(action).queue();
    }

    @Test
    void appendButtonWhenMessageIsValidTest() {
        // GIVEN
        long messageId = 123456789;
        long authorId = 987654321;

        SelfUser selfUser = Mockito.mock(SelfUser.class);
        JDA jda = Mockito.mock(JDA.class);
        User author = Mockito.mock(User.class);
        RestAction<Message> latestMessageAction = Mockito.mock(RestAction.class);
        ArgumentCaptor<Collection<ActionRow>> captor = ArgumentCaptor.forClass(Collection.class);

        when(channel.getLatestMessageIdLong()).thenReturn(messageId);
        when(channel.retrieveMessageById(anyLong())).thenReturn(latestMessageAction);
        when(latestMessageAction.complete()).thenReturn(message);

        when(message.getAuthor()).thenReturn(author);
        when(author.getIdLong()).thenReturn(authorId);

        when(channel.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(selfUser);
        when(selfUser.getIdLong()).thenReturn(authorId);

        when(message.getActionRows()).thenReturn(List.of());
        when(message.editMessageComponents(anyCollection())).thenReturn(action);
        // WHEN
        utils.sendOrAppendButton(channel, button);

        // THEN
        verify(channel).retrieveMessageById(messageId);
        verify(channel, never()).sendMessage(anyString());
        verify(message).editMessageComponents(captor.capture());
        verify(action).queue();

        Collection<ActionRow> result = captor.getValue();
        assertThat(result).hasSize(1);
        ActionRow actionRow = result.stream().findFirst().orElseThrow();
        assertThat(actionRow.getComponents()).containsExactly(button);
    }

    @Test
    void appendButtonToMessageTest() {
        // GIVEN
        ArgumentCaptor<Collection<ActionRow>> captor = ArgumentCaptor.forClass(Collection.class);

        when(message.getActionRows()).thenReturn(List.of());
        when(message.editMessageComponents(anyCollection())).thenReturn(action);
        // WHEN
        utils.appendButtonToMessage(button, message);

        // THEN
        verify(message).editMessageComponents(captor.capture());
        verify(action).queue();

        Collection<ActionRow> result = captor.getValue();
        assertThat(result).hasSize(1);
        ActionRow actionRow = result.stream().findFirst().orElseThrow();
        assertThat(actionRow.getComponents()).containsExactly(button);
    }

    @Test
    void appendButtonToOtherButtonsInMessageTest() {
        // GIVEN

        Button placeholderButton = Mockito.mock(Button.class);
        when(placeholderButton.getType()).thenReturn(Component.Type.BUTTON);

        List<ActionRow> currentButtons = List.of(ActionRow.of(placeholderButton));
        List<Button> expected = List.of(placeholderButton, button);

        ArgumentCaptor<Collection<ActionRow>> captor = ArgumentCaptor.forClass(Collection.class);

        when(message.getActionRows()).thenReturn(currentButtons);
        when(message.editMessageComponents(anyCollection())).thenReturn(action);
        // WHEN
        utils.appendButtonToMessage(button, message);

        // THEN
        verify(message).editMessageComponents(captor.capture());
        verify(action).queue();

        Collection<ActionRow> result = captor.getValue();
        assertThat(result).hasSize(1);
        ActionRow actionRow = result.stream().findFirst().orElseThrow();
        assertThat(actionRow.getComponents()).containsExactlyElementsOf(expected);
    }

}

package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketServiceTests {

    private TicketService service;

    private Member member;
    private TextChannel channel;

    @BeforeEach
    void initTest() {
        service = new TicketService();

        member = Mockito.mock(Member.class);
        channel = Mockito.mock(TextChannel.class);
    }

    @Test
    void sendTicketButtonInChannelTest() {
        // GIVEN
        String label = "My Button !";
        Button expectedButton = Button.of(ButtonStyle.SECONDARY, "ticket-creation", label, Emoji.fromUnicode("U+1F39F"));
        MessageAction action = Mockito.mock(MessageAction.class);

        when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(true);
        when(channel.sendMessage("⌄")).thenReturn(action);
        when(action.setActionRow(expectedButton)).thenReturn(action);
        // WHEN
        service.sendTicketButtonInChannel(member, channel, label);

        // THEN
        verify(member).hasPermission(Permission.MANAGE_SERVER);
        verify(channel).sendMessage("⌄");
        verify(action).setActionRow(expectedButton);
        verify(action).queue();
    }

    @Test
    void refuseUnauthorizedTicketButtonInChannelTest() {
        // GIVEN
        String label = "My Button !";

        when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.sendTicketButtonInChannel(member, channel, label));

        // THEN
        verify(channel, never()).sendMessage(any(String.class));
    }

    // TODO Write tests

}

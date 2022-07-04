package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static fr.seynox.saejinaapp.services.TicketService.TICKETS_CATEGORY_NAME;
import static fr.seynox.saejinaapp.services.TicketService.TICKET_CREATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
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
    void sendTicketCreationButtonInChannelTest() {
        // GIVEN
        String label = "My Button !";
        Button expectedButton = Button.of(ButtonStyle.SECONDARY, "ticket-creation", label, Emoji.fromUnicode("U+1F39F"));
        MessageAction action = Mockito.mock(MessageAction.class);

        when(member.hasPermission(Permission.MANAGE_CHANNEL)).thenReturn(true);
        when(channel.sendMessage("⌄")).thenReturn(action);
        when(action.setActionRow(expectedButton)).thenReturn(action);
        // WHEN
        service.sendTicketButtonInChannel(member, channel, label);

        // THEN
        verify(member).hasPermission(Permission.MANAGE_CHANNEL);
        verify(channel).sendMessage("⌄");
        verify(action).setActionRow(expectedButton);
        verify(action).queue();
    }

    @Test
    void refuseUnauthorizedTicketButtonInChannelTest() {
        // GIVEN
        String label = "My Button !";

        when(member.hasPermission(Permission.MANAGE_CHANNEL)).thenReturn(false);
        // WHEN
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> service.sendTicketButtonInChannel(member, channel, label));

        // THEN
        verify(channel, never()).sendMessage(any(String.class));
    }

    @Test
    void showTicketCreationFormTest() {
        // GIVEN
        Button button = Mockito.mock(Button.class);
        ComponentInteraction interaction = Mockito.mock(ComponentInteraction.class);
        ModalCallbackAction action = Mockito.mock(ModalCallbackAction.class);

        ArgumentCaptor<Modal> captor = ArgumentCaptor.forClass(Modal.class);
        String buttonLabel = "Click here to create a ticket";

        when(interaction.replyModal(any(Modal.class))).thenReturn(action);
        when(button.getLabel()).thenReturn(buttonLabel);
        // WHEN
        service.showTicketCreationForm(member, button, interaction);

        // THEN
        verify(interaction).replyModal(captor.capture());
        Modal modal = captor.getValue();
        assertThat(modal.getId()).isEqualTo(TICKET_CREATION_ID);
        assertThat(modal.getTitle()).isEqualTo(buttonLabel);
        assertThat(modal.getActionRows()).hasSize(2);
    }

    @Test
    void refuseTicketCreationFormInDMTest() {
        // GIVEN
        Button button = Mockito.mock(Button.class);
        ComponentInteraction interaction = Mockito.mock(ComponentInteraction.class);
        ReplyCallbackAction action = Mockito.mock(ReplyCallbackAction.class);

        String expectedErrorMessage = "Error ! Ticket buttons are only usable in servers";

        when(interaction.reply(expectedErrorMessage)).thenReturn(action);
        when(action.setEphemeral(true)).thenReturn(action);
        // WHEN
        service.showTicketCreationForm(null, button, interaction);

        // THEN
        verify(interaction).reply(expectedErrorMessage);
        verify(action).setEphemeral(true);
        verify(action).queue();
    }

    @Test
    void getTicketChannelTest() {
        // GIVEN
        Guild guild = Mockito.mock(Guild.class);
        Role everyoneRole = Mockito.mock(Role.class);
        Category category = Mockito.mock(Category.class);
        ChannelAction<TextChannel> action = Mockito.mock(ChannelAction.class);

        TextChannel placeholderChannel = Mockito.mock(TextChannel.class);
        List<TextChannel> channels = List.of(placeholderChannel);

        String expectedTicketChannelName = "ticket-2";

        when(guild.getPublicRole()).thenReturn(everyoneRole);
        when(guild.getCategoriesByName(TICKETS_CATEGORY_NAME, false)).thenReturn(List.of(category));
        when(category.getTextChannels()).thenReturn(channels);
        when(category.createTextChannel(expectedTicketChannelName)).thenReturn(action);
        // WHEN
        service.createTicketChannel(guild);

        // THEN
        verify(guild).getPublicRole();
        verify(guild, never()).createCategory(TICKETS_CATEGORY_NAME);
        verify(category).createTextChannel(expectedTicketChannelName);
        verify(action).complete();
    }

    @Test
    void createTicketChannelTest() {
        // GIVEN
        Guild guild = Mockito.mock(Guild.class);
        Role everyoneRole = Mockito.mock(Role.class);
        Category category = Mockito.mock(Category.class);
        ChannelAction<Category> categoryAction = Mockito.mock(ChannelAction.class);
        ChannelAction<TextChannel> channelAction = Mockito.mock(ChannelAction.class);

        String expectedTicketChannelName = "ticket-1";

        when(guild.getPublicRole()).thenReturn(everyoneRole);
        when(guild.getCategoriesByName(TICKETS_CATEGORY_NAME, false)).thenReturn(List.of());
        when(guild.createCategory(TICKETS_CATEGORY_NAME)).thenReturn(categoryAction);
        when(categoryAction.addPermissionOverride(any(), any(), any())).thenReturn(categoryAction);
        when(categoryAction.complete()).thenReturn(category);
        when(category.createTextChannel(expectedTicketChannelName)).thenReturn(channelAction);
        // WHEN
        service.createTicketChannel(guild);

        // THEN
        verify(guild).getPublicRole();
        verify(guild).createCategory(TICKETS_CATEGORY_NAME);
        verify(categoryAction).addPermissionOverride(everyoneRole, List.of(), List.of(Permission.VIEW_CHANNEL));
        verify(categoryAction).complete();
        verify(category).createTextChannel(expectedTicketChannelName);
    }

    @Test
    void sendTicketToChannelTest() {
        // GIVEN
        ModalInteractionEvent event = Mockito.mock(ModalInteractionEvent.class);

        String memberName = "Bob";
        String memberId = "123456789";
        String avatarUrl = "https://example.com/avatar.png";

        String expectedName = "%s (@%s)".formatted(memberName, memberId);

        ModalMapping subjectMapping = Mockito.mock(ModalMapping.class);
        ModalMapping bodyMapping = Mockito.mock(ModalMapping.class);
        String subject = "This is my example ticket";
        String body = "Lorem ipsum dolor sit amet, consectetur adipiscing elit";

        MessageAction messageAction = Mockito.mock(MessageAction.class);
        ReplyCallbackAction replyAction = Mockito.mock(ReplyCallbackAction.class);

        String successMessage = "Your ticket was submitted !";

        ArgumentCaptor<MessageEmbed> embedCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        ArgumentCaptor<ActionRow> actionCaptor = ArgumentCaptor.forClass(ActionRow.class);

        when(member.getEffectiveName()).thenReturn(memberName);
        when(member.getId()).thenReturn(memberId);
        when(member.getEffectiveAvatarUrl()).thenReturn(avatarUrl);
        when(event.getValue("subject")).thenReturn(subjectMapping);
        when(event.getValue("body")).thenReturn(bodyMapping);
        when(subjectMapping.getAsString()).thenReturn(subject);
        when(bodyMapping.getAsString()).thenReturn(body);
        when(channel.sendMessageEmbeds(any(MessageEmbed.class))).thenReturn(messageAction);
        when(messageAction.setActionRows(any(ActionRow.class))).thenReturn(messageAction);
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(true)).thenReturn(replyAction);
        // WHEN
        service.sendTicketToChannel(member, channel, event);

        // THEN
        verify(channel).sendMessageEmbeds(embedCaptor.capture());
        verify(messageAction).setActionRows(actionCaptor.capture());
        verify(messageAction).queue();
        verify(event).reply(successMessage);
        verify(replyAction).setEphemeral(true);
        verify(replyAction).queue();

        ActionRow actionRow = actionCaptor.getValue();
        assertThat(actionRow.getComponents()).hasSize(2);

        MessageEmbed embed = embedCaptor.getValue();
        MessageEmbed.AuthorInfo author = embed.getAuthor();
        assertThat(author).isNotNull();
        assertThat(author.getName()).isEqualTo(expectedName);
        assertThat(author.getIconUrl()).isEqualTo(avatarUrl);
        assertThat(embed.getTitle()).isEqualTo(subject);
        assertThat(embed.getDescription()).isEqualTo(body);
    }

    @Test
    void inviteUserToTicketChannelTest() {
        // GIVEN
        ComponentInteraction interaction = Mockito.mock(ComponentInteraction.class);
        Guild guild = Mockito.mock(Guild.class);
        RestAction<Member> memberAction = Mockito.mock(RestAction.class);
        TextChannelManager manager = Mockito.mock(TextChannelManager.class);
        ReplyCallbackAction replyAction = Mockito.mock(ReplyCallbackAction.class);

        long userId = 123456789;
        String memberMention = "<@%s>".formatted(userId);
        String successMessage = "The ticket owner (%s) has been invited to the channel !".formatted(memberMention);

        when(channel.getGuild()).thenReturn(guild);
        when(guild.retrieveMemberById(anyString())).thenReturn(memberAction);
        when(memberAction.complete()).thenReturn(member);
        when(channel.getManager()).thenReturn(manager);
        when(manager.putMemberPermissionOverride(anyLong(), anyList(), anyList())).thenReturn(manager);
        when(member.getIdLong()).thenReturn(userId);
        when(member.getAsMention()).thenReturn(memberMention);
        when(interaction.reply(anyString())).thenReturn(replyAction);
        // WHEN
        service.inviteUserToTicketChannel(String.valueOf(userId), channel, interaction);

        // THEN
        verify(interaction).reply(successMessage);
        verify(replyAction).queue();
        verify(manager).putMemberPermissionOverride(userId, List.of(Permission.VIEW_CHANNEL), List.of());
        verify(manager).queue();
    }

    @Test
    void refuseUnknownUserToTicketChannelTest() {
        // GIVEN
        ComponentInteraction interaction = Mockito.mock(ComponentInteraction.class);
        Guild guild = Mockito.mock(Guild.class);
        RestAction<Member> memberAction = Mockito.mock(RestAction.class);
        ReplyCallbackAction replyAction = Mockito.mock(ReplyCallbackAction.class);

        long userId = 123456789;
        String errorMessage = "Error ! The user could not be found in the server";

        when(channel.getGuild()).thenReturn(guild);
        when(guild.retrieveMemberById(anyString())).thenReturn(memberAction);
        when(memberAction.complete()).thenReturn(null);
        when(interaction.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(true)).thenReturn(replyAction);
        // WHEN
        service.inviteUserToTicketChannel(String.valueOf(userId), channel, interaction);

        // THEN
        verify(interaction).reply(errorMessage);
        verify(replyAction).setEphemeral(true);
        verify(replyAction).queue();
        verify(channel, never()).getManager();
    }

}

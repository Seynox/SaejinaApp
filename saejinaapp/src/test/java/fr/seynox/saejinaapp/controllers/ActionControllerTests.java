package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.services.DiscordService;
import fr.seynox.saejinaapp.services.MemberAccessService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActionController.class)
class ActionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiscordService service;

    @MockBean
    private MemberAccessService accessService;

    private String userId;
    private long serverId;
    private long channelId;

    private Member member;
    private TextChannel channel;

    @BeforeEach
    void initTest() {
        userId = "456123";
        serverId = 123456;
        channelId = 654321;

        member = Mockito.mock(Member.class);
        channel = Mockito.mock(TextChannel.class);
    }

    @Test
    void showMessageFormTest() throws Exception {
        // GIVEN
        String channelName = "my-channel";

        String requestUri = "/%s/%s/send_message"
                .formatted(serverId, channelId);
        RequestBuilder request = get(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))); // "sub" is the default nameAttributeKey

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(accessService.getWritableServerTextChannel(member, channelId)).thenReturn(channel);
        when(channel.getName()).thenReturn(channelName);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("message"))
                .andExpect(model().attribute("channelName", channelName));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(channel).getName();
    }

    @Test
    void refuseMessageFormUnauthenticatedTest() throws Exception {
        // GIVEN
        String requestUri = "/%s/%s/send_message"
                .formatted(serverId, channelId);
        RequestBuilder request = get(requestUri);

        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());

        // THEN
        verify(accessService, never()).getServerMember(any(), any());
        verify(accessService, never()).getWritableServerTextChannel(any(), any());
        verify(service, never()).sendMessageInChannel(any(), any(), any());
    }

    @Test
    void sendMessageInChannelTest() throws Exception {
        // GIVEN
        String content = "Hello world !";
        String body = "content=%s"
                .formatted(URLEncoder.encode(content, StandardCharsets.UTF_8));

        String requestUri = "/%s/%s/send_message"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))) // "sub" is the default nameAttributeKey
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(body)
                .with(csrf());

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(accessService.getWritableServerTextChannel(member, channelId)).thenReturn(channel);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("?success"));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(service).sendMessageInChannel(member, channel, content);
    }

    @Test
    void refuseMessageInChannelUnauthenticatedTest() throws Exception {
        // GIVEN
        String content = "Hello world !";
        String body = "content=%s"
                .formatted(URLEncoder.encode(content, StandardCharsets.UTF_8));

        String requestUri = "/%s/%s/send_message"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(body)
                .with(csrf());

        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());

        // THEN
        verify(accessService, never()).getServerMember(any(), any());
        verify(accessService, never()).getWritableServerTextChannel(any(), any());
        verify(service, never()).sendMessageInChannel(any(), any(), any());
    }

    @Test
    void refuseBlankMessageInChannelTest() throws Exception {
        // GIVEN
        String channelName = "my-channel";

        String content = "   ";
        String body = "content=%s"
                .formatted(URLEncoder.encode(content, StandardCharsets.UTF_8));

        String requestUri = "/%s/%s/send_message"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))) // "sub" is the default nameAttributeKey
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(body)
                .with(csrf());

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(accessService.getWritableServerTextChannel(member, channelId)).thenReturn(channel);
        when(channel.getName()).thenReturn(channelName);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("message"))
                .andExpect(model().attribute("channelName", channelName))
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrorCode("message", "content", "NotBlank"));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(service, never()).sendMessageInChannel(any(), any(), any());
    }

    @Test
    void refuseMessageTooLongInChannelTest() throws Exception {
        // GIVEN
        String channelName = "my-channel";

        String content = "A".repeat(2001);
        String body = "content=%s"
                .formatted(URLEncoder.encode(content, StandardCharsets.UTF_8));

        String requestUri = "/%s/%s/send_message"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))) // "sub" is the default nameAttributeKey
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(body)
                .with(csrf());

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(accessService.getWritableServerTextChannel(member, channelId)).thenReturn(channel);
        when(channel.getName()).thenReturn(channelName);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("message"))
                .andExpect(model().attribute("channelName", channelName))
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrorCode("message", "content", "Size"));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(service, never()).sendMessageInChannel(any(), any(), any());
    }

    @Test
    void showTicketButtonFormTest() throws Exception {
        // GIVEN
        String channelName = "my-channel";

        String requestUri = "/%s/%s/send_ticket_button"
                .formatted(serverId, channelId);

        RequestBuilder request = get(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))); // "sub" is the default nameAttributeKey

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(accessService.getWritableServerTextChannel(member, channelId)).thenReturn(channel);
        when(channel.getName()).thenReturn(channelName);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("buttonLabel"))
                .andExpect(model().attribute("channelName", channelName))
                .andExpect(view().name("/action/ticket_button"));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
    }

    @Test
    void refuseTicketButtonFormUnauthenticatedTest() throws Exception {
        // GIVEN
        String requestUri = "/%s/%s/send_ticket_button"
                .formatted(serverId, channelId);

        RequestBuilder request = get(requestUri);

        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());

        // THEN
        verify(accessService, never()).getServerMember(any(), any());
        verify(accessService, never()).getWritableServerTextChannel(any(), any());
    }

    @Test
    void sendTicketButtonTest() throws Exception {
        // GIVEN
        String content = "This is my button label !";
        String body = "content=%s"
                .formatted(URLEncoder.encode(content, StandardCharsets.UTF_8));

        String requestUri = "/%s/%s/send_ticket_button"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))) // "sub" is the default nameAttributeKey
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(body)
                .with(csrf());

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(accessService.getWritableServerTextChannel(member, channelId)).thenReturn(channel);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("?success"));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(service).sendTicketButtonInChannel(member, channel, content);
    }

    @Test
    void refuseTicketButtonWithLabelTooLongTest() throws Exception {
        // GIVEN
        String content = "A".repeat(81);
        String body = "content=%s"
                .formatted(URLEncoder.encode(content, StandardCharsets.UTF_8));

        String requestUri = "/%s/%s/send_ticket_button"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))) // "sub" is the default nameAttributeKey
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(body)
                .with(csrf());

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(accessService.getWritableServerTextChannel(member, channelId)).thenReturn(channel);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrorCode("buttonLabel", "content", "Size"));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(service, never()).sendTicketButtonInChannel(any(), any(), any());
    }

    @Test
    void refuseTicketButtonWithBlankLabelTest() throws Exception {
        // GIVEN
        String content = "     ";
        String body = "content=%s"
                .formatted(URLEncoder.encode(content, StandardCharsets.UTF_8));

        String requestUri = "/%s/%s/send_ticket_button"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))) // "sub" is the default nameAttributeKey
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(body)
                .with(csrf());

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(accessService.getWritableServerTextChannel(member, channelId)).thenReturn(channel);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrorCode("buttonLabel", "content", "NotBlank"));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(service, never()).sendTicketButtonInChannel(any(), any(), any());
    }

}

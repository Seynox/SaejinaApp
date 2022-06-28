package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannel;
import fr.seynox.saejinaapp.models.TextChannelAction;
import fr.seynox.saejinaapp.services.MemberAccessService;
import fr.seynox.saejinaapp.services.DiscordService;
import net.dv8tion.jda.api.entities.Member;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SelectionController.class)
class SelectionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiscordService service;

    @MockBean
    private MemberAccessService accessService;

    @Test
    void showServerSelectionTest() throws Exception {
        // GIVEN
        String userId = "123456789";

        String requestUrl = "/";
        RequestBuilder request = get(requestUrl)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))); // "sub" is the default nameAttributeKey

        List<Server> serverList = List.of(
                new Server("Server One", null, 123456L),
                new Server("Server Two", null, 987654L)
        );

        when(service.getUserServers(userId)).thenReturn(serverList);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attribute("serverList", serverList));

        // THEN
        verify(service, times(1)).getUserServers(userId);
    }

    @Test
    void refuseServerSelectionUnauthenticatedTest() throws Exception {
        // GIVEN
        String requestUrl = "/";
        RequestBuilder request = get(requestUrl);

        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());

        // THEN
        verify(service, never()).getUserServers(any());
    }

    @Test
    void showChannelSelectionTest() throws Exception {
        // GIVEN
        String userId = "456123";
        long serverId = 123456;

        String requestUri = "/" + serverId;
        RequestBuilder request = get(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))); // "sub" is the default nameAttributeKey

        List<TextChannel> channels = List.of(
                new TextChannel(123456L, "Channel One"),
                new TextChannel(987654L, "Channel Two")
        );

        Member member = Mockito.mock(Member.class);

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(service.getVisibleServerTextChannels(member)).thenReturn(channels);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attribute("channels", channels))
                .andExpect(model().attribute("serverId", serverId));

        // THEN
        verify(accessService, times(1)).getServerMember(userId, serverId);
        verify(service, times(1)).getVisibleServerTextChannels(member);
    }

    @Test
    void refuseChannelSelectionOnOtherServerTest() throws Exception {
        // GIVEN
        String userId = "456123";
        long serverId = 123456;

        String requestUri = "/" + serverId;
        RequestBuilder request = get(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))); // "sub" is the default nameAttributeKey

        when(accessService.getServerMember(userId, serverId)).thenThrow(new ResourceNotAccessibleException());
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("/exception/error"));

        // THEN
        verify(accessService, times(1)).getServerMember(userId, serverId);
        verify(service, never()).getVisibleServerTextChannels(any());
    }

    @Test
    void refuseChannelSelectionUnauthenticatedTest() throws Exception {
        // GIVEN
        long serverId = 123456;

        String requestUri = "/" + serverId;
        RequestBuilder request = get(requestUri);

        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());

        // THEN
        verify(accessService, never()).getServerMember(any(), any());
        verify(service, never()).getVisibleServerTextChannels(any());
    }

    @Test
    void showActionSelectionTest() throws Exception {
        // GIVEN
        long serverId = 123456;
        long channelId = 654321;

        String userId = "123654";

        Member member = Mockito.mock(Member.class);
        net.dv8tion.jda.api.entities.TextChannel channel = Mockito.mock(net.dv8tion.jda.api.entities.TextChannel.class);

        String requestUri = "/%s/%s".formatted(serverId, channelId);
        RequestBuilder request = get(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))); // "sub" is the default nameAttributeKey

        List<TextChannelAction> actions = Arrays.asList(TextChannelAction.values());

        when(accessService.getServerMember(userId, serverId)).thenReturn(member);
        when(accessService.getServerTextChannel(member, channelId)).thenReturn(channel);
        when(service.getPossibleActionsForChannel(member, channel)).thenReturn(actions);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("/selection/actions"))
                .andExpect(model().attribute("actions", actions));

        // THEN
        verify(accessService, times(1)).getServerMember(userId, serverId);
        verify(accessService, times(1)).getServerTextChannel(member, channelId);
        verify(service, times(1)).getPossibleActionsForChannel(member, channel);
    }

    @Test
    void refuseActionSelectionUnauthenticatedTest() throws Exception {
        // GIVEN
        String serverId = "123456";
        String channelId = "654321";

        String requestUri = "/%s/%s".formatted(serverId, channelId);
        RequestBuilder request = get(requestUri);

        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());

        // THEN
        verify(accessService, never()).getServerMember(any(), any());
        verify(accessService, never()).getServerTextChannel(any(), any());
        verify(service, never()).getPossibleActionsForChannel(any(), any());
    }

}

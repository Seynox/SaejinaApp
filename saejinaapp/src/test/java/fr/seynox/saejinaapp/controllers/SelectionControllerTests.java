package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannel;
import fr.seynox.saejinaapp.services.DiscordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

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

        when(service.isUserInServer(userId, serverId)).thenReturn(true);
        when(service.getServerTextChannels(serverId)).thenReturn(channels);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attribute("channels", channels))
                .andExpect(model().attribute("serverId", serverId));

        // THEN
        verify(service, times(1)).isUserInServer(userId, serverId);
        verify(service, times(1)).getServerTextChannels(serverId);
    }

    @Test
    void refuseChannelSelectionOnOtherServerTest() throws Exception {
        // GIVEN
        String userId = "456123";
        long serverId = 123456;

        String requestUri = "/" + serverId;
        RequestBuilder request = get(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))); // "sub" is the default nameAttributeKey

        when(service.isUserInServer(userId, serverId)).thenReturn(false);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // THEN
        verify(service, times(1)).isUserInServer(userId, serverId);
        verify(service, never()).getServerTextChannels(any());
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
        verify(service, never()).isUserInServer(any(), eq(serverId));
        verify(service, never()).getServerTextChannels(any());
    }

}

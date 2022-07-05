package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.models.Selectable;
import fr.seynox.saejinaapp.models.SelectableImpl;
import fr.seynox.saejinaapp.services.MemberAccessService;
import fr.seynox.saejinaapp.services.RoleService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
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

import java.util.List;

import static fr.seynox.saejinaapp.models.ViewTemplateConsts.CHANNEL_NAME_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
class RoleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService service;

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
    void showRoleButtonFormTest() throws Exception {
        // GIVEN
        String channelName = "roles-channel";

        List<Selectable> roles = List.of(
                new SelectableImpl(1L, "Role One"),
                new SelectableImpl(2L, "Role Two")
        );

        String requestUri = "/%s/%s/send_role_button"
                .formatted(serverId, channelId);

        RequestBuilder request = get(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))); // "sub" is the default nameAttributeKey

        when(channel.getName()).thenReturn(channelName);
        when(accessService.getServerMember(anyString(), anyLong())).thenReturn(member);
        when(accessService.getWritableServerTextChannel(any(Member.class), anyLong())).thenReturn(channel);
        when(service.getAssignableRolesForMember(any(Member.class))).thenReturn(roles);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attribute("roles", roles))
                .andExpect(model().attributeExists("roleId"))
                .andExpect(model().attribute(CHANNEL_NAME_ATTRIBUTE, channelName));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(service).getAssignableRolesForMember(member);
    }

    @Test
    void refuseRoleButtonUnauthenticatedTest() throws Exception {
        // GIVEN
        String requestUri = "/%s/%s/send_role_button"
                .formatted(serverId, channelId);

        RequestBuilder request = get(requestUri);

        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());

        // THEN
        verify(accessService, never()).getServerMember(anyString(), anyLong());
        verify(service, never()).getAssignableRolesForMember(any());
    }

    @Test
    void sendRoleButtonTest() throws Exception {
        // GIVEN
        String channelName = "roles-channel";

        long roleId = 123456789;
        Role role = Mockito.mock(Role.class);

        String body = "id=%s".formatted(roleId);

        String requestUri = "/%s/%s/send_role_button"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))) // "sub" is the default nameAttributeKey
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(body)
                .with(csrf());

        when(accessService.getServerMember(anyString(), anyLong())).thenReturn(member);
        when(accessService.getWritableServerTextChannel(any(Member.class), anyLong())).thenReturn(channel);
        when(channel.getName()).thenReturn(channelName);
        when(accessService.getAssignableServerRole(any(Member.class), anyLong())).thenReturn(role);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("?success"));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(accessService).getAssignableServerRole(member, roleId);
        verify(service).sendRoleButtonInChannel(member, channel, role);
    }

    @Test
    void refuseToSendRoleButtonUnauthenticatedTest() throws Exception {
        // GIVEN
        String requestUri = "/%s/%s/send_role_button"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .with(csrf());

        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());

        // THEN
        verify(accessService, never()).getServerMember(anyString(), anyLong());
        verify(service, never()).sendRoleButtonInChannel(any(), any(), any());
    }

    @Test
    void refuseToSendNullRoleButtonTest() throws Exception {
        // GIVEN
        String channelName = "roles-channel";

        String requestUri = "/%s/%s/send_role_button"
                .formatted(serverId, channelId);

        RequestBuilder request = post(requestUri)
                .with(oauth2Login().attributes(attrs -> attrs.put("sub", userId))) // "sub" is the default nameAttributeKey
                .with(csrf());

        when(accessService.getServerMember(anyString(), anyLong())).thenReturn(member);
        when(accessService.getWritableServerTextChannel(any(Member.class), anyLong())).thenReturn(channel);
        when(channel.getName()).thenReturn(channelName);
        // WHEN
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("roleId"))
                .andExpect(model().attribute(CHANNEL_NAME_ATTRIBUTE, channelName))
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrorCode("roleId", "id", "NotNull"));

        // THEN
        verify(accessService).getServerMember(userId, serverId);
        verify(accessService).getWritableServerTextChannel(member, channelId);
        verify(service, never()).sendRoleButtonInChannel(any(), any(), any());
    }

}

package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import fr.seynox.saejinaapp.models.IdRequest;
import fr.seynox.saejinaapp.models.Selectable;
import fr.seynox.saejinaapp.services.MemberAccessService;
import fr.seynox.saejinaapp.services.RoleService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static fr.seynox.saejinaapp.models.ViewTemplateConsts.CHANNEL_NAME_ATTRIBUTE;

@Controller
@RequestMapping("/panel/{serverId}/{channelId}/send_role_button")
public class RoleController {
    private final RoleService service;
    private final MemberAccessService accessService;

    public RoleController(RoleService service, MemberAccessService accessService) {
        this.service = service;
        this.accessService = accessService;
    }

    /**
     * Show the form used to send a button assigning a role to the user clicking it
     * @param serverId The channel's server
     * @param channelId The channel to send the role button to
     * @param principal The logged-in user
     * @throws ResourceNotAccessibleException If the server/channel is not accessible/writable for the user/bot
     */
    @GetMapping
    public String showRoleButtonForm(@PathVariable Long serverId, @PathVariable Long channelId, @AuthenticationPrincipal OAuth2User principal, Model model) {
        String userId = principal.getName();
        Member member = accessService.getServerMember(userId, serverId);
        TextChannel channel = accessService.getWritableServerTextChannel(member, channelId);

        List<Selectable> roles = service.getAssignableRolesForMember(member);

        model.addAttribute("roles", roles);
        model.addAttribute("roleId", new IdRequest());
        model.addAttribute(CHANNEL_NAME_ATTRIBUTE, channel.getName());

        return "action/role_button";
    }

    /**
     * Send a button assigning the given role to the user clicking it
     * @param roleRequest The request containing the ID of the role to assign
     * @param result The request validation result
     * @param serverId The channel server
     * @param channelId The channel to send the button to
     * @param principal The logged-in user
     * @throws ResourceNotAccessibleException If the server/channel is not accessible/writable for the user/bot, or if the role does not exist
     * @throws PermissionException If the user does not have the permission to assign the given role to other users
     * @return If successful, redirect to {@link RoleController#showRoleButtonForm(Long, Long, OAuth2User, Model)} with a success parameter
     */
    @PostMapping
    public String sendRoleButton(@Validated @ModelAttribute("roleId") IdRequest roleRequest, BindingResult result, @PathVariable Long serverId, @PathVariable Long channelId, @AuthenticationPrincipal OAuth2User principal, Model model) {
        String userId = principal.getName();
        Member member = accessService.getServerMember(userId, serverId);
        TextChannel channel = accessService.getWritableServerTextChannel(member, channelId);

        if(result.hasErrors()) {
            model.addAttribute("roleId", roleRequest);
            model.addAttribute(CHANNEL_NAME_ATTRIBUTE, channel.getName());
            return "action/role_button";
        }

        Long roleId = roleRequest.getId();
        Role role = accessService.getAssignableServerRole(member, roleId);

        service.sendRoleButtonInChannel(member, channel, role);

        return "redirect:?success";
    }

}

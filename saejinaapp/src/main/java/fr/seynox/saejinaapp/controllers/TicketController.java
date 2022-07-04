package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import fr.seynox.saejinaapp.models.StringRequest;
import fr.seynox.saejinaapp.services.MemberAccessService;
import fr.seynox.saejinaapp.services.TicketService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.groups.Default;

import static fr.seynox.saejinaapp.models.ViewTemplateConsts.CHANNEL_NAME_ATTRIBUTE;

@Controller
@RequestMapping("/{serverId}/{channelId}/send_ticket_button")
public class TicketController {

    private final TicketService service;
    private final MemberAccessService accessService;

    public TicketController(TicketService service, MemberAccessService accessService) {
        this.service = service;
        this.accessService = accessService;
    }

    /**
     * Show the form used to send a button used to create tickets.
     * Doesn't check if the user is allowed to send ticket buttons
     * @param serverId The channel's server
     * @param channelId The channel to send the button to
     * @param principal The logged-in user
     * @throws ResourceNotAccessibleException When the server/channel is not accessible/writable for the user/bot.
     * @return The path to the Thymeleaf template
     */
    @GetMapping
    public String showTicketButtonForm(@PathVariable Long serverId, @PathVariable Long channelId, @AuthenticationPrincipal OAuth2User principal, Model model) {
        String userId = principal.getName();
        Member member = accessService.getServerMember(userId, serverId);
        TextChannel channel = accessService.getWritableServerTextChannel(member, channelId);

        model.addAttribute("buttonLabel", new StringRequest());
        model.addAttribute(CHANNEL_NAME_ATTRIBUTE, channel.getName());

        return "/action/ticket_button";
    }

    /**
     * Send a button used to create tickets, in the given channel
     * @param buttonLabel The text to put on the button
     * @param result The label validation results
     * @param serverId The channel's server
     * @param channelId The channel to send the button to
     * @param principal The logged-in user
     * @throws ResourceNotAccessibleException When the server/channel is not accessible/writable for the user/bot.
     * @throws PermissionException If the member is not allowed to send ticket buttons on the server.
     * @return If successful, redirect to {@link TicketController#showTicketButtonForm(Long, Long, OAuth2User, Model)} with a success parameter
     */
    @PostMapping
    public String sendTicketButton(@Validated({Default.class, Button.class}) @ModelAttribute("buttonLabel") StringRequest buttonLabel, BindingResult result, @PathVariable Long serverId, @PathVariable Long channelId, @AuthenticationPrincipal OAuth2User principal, Model model) {
        String userId = principal.getName();
        Member member = accessService.getServerMember(userId, serverId);
        TextChannel channel = accessService.getWritableServerTextChannel(member, channelId);

        if(result.hasErrors()) {
            model.addAttribute("buttonLabel", buttonLabel);
            model.addAttribute(CHANNEL_NAME_ATTRIBUTE, channel.getName());
            return "/action/ticket_button";
        }

        service.sendTicketButtonInChannel(member, channel, buttonLabel.getContent());

        return "redirect:?success";
    }


}

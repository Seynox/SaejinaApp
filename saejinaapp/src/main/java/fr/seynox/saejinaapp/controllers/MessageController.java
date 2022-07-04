package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import fr.seynox.saejinaapp.models.StringRequest;
import fr.seynox.saejinaapp.services.DiscordService;
import fr.seynox.saejinaapp.services.MemberAccessService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
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
@RequestMapping("/{serverId}/{channelId}/send_message")
public class MessageController {

    private final MemberAccessService accessService;
    private final DiscordService service;

    public MessageController(MemberAccessService accessService, DiscordService service) {
        this.accessService = accessService;
        this.service = service;
    }

    /**
     * Show the form used to send a message
     * @param serverId The channel's server
     * @param channelId The channel to send the message to
     * @param principal The logged-in user
     * @throws ResourceNotAccessibleException When the server/channel is not accessible/writable for the user/bot.
     * @return The path to the Thymeleaf template
     */
    @GetMapping
    public String showMessageForm(@PathVariable Long serverId, @PathVariable Long channelId, @AuthenticationPrincipal OAuth2User principal, Model model) {
        String userId = principal.getName();
        Member member = accessService.getServerMember(userId, serverId);
        TextChannel channel = accessService.getWritableServerTextChannel(member, channelId);

        model.addAttribute("message", new StringRequest());
        model.addAttribute(CHANNEL_NAME_ATTRIBUTE, channel.getName());

        return "/action/message";
    }

    /**
     * Send a message in the given channel
     * @param message The message to send
     * @param result The message validation results
     * @param serverId The channel's server
     * @param channelId The channel to send the message to
     * @param principal The logged-in user
     * @throws ResourceNotAccessibleException When the server/channel is not accessible/writable for the user/bot.
     * @return If successful, redirect to {@link MessageController#showMessageForm(Long, Long, OAuth2User, Model)} with a success parameter
     */
    @PostMapping
    public String sendMessageInChannel(@Validated({Default.class, Message.class}) @ModelAttribute("message") StringRequest message, BindingResult result, @PathVariable Long serverId, @PathVariable Long channelId, @AuthenticationPrincipal OAuth2User principal, Model model) {

        String userId = principal.getName();
        Member member = accessService.getServerMember(userId, serverId);
        TextChannel channel = accessService.getWritableServerTextChannel(member, channelId);

        if(result.hasErrors()) {
            model.addAttribute("message", message);
            model.addAttribute(CHANNEL_NAME_ATTRIBUTE, channel.getName());
            return "/action/message";
        }

        service.sendMessageInChannel(member, channel, message.getContent());

        return "redirect:?success";
    }

}

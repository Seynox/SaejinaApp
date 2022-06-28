package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.models.DiscordMessage;
import fr.seynox.saejinaapp.services.DiscordService;
import fr.seynox.saejinaapp.services.MemberAccessService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/{serverId}/{channelId}")
public class ActionController {

    private final MemberAccessService accessService;
    private final DiscordService service;

    public ActionController(MemberAccessService accessService, DiscordService service) {
        this.accessService = accessService;
        this.service = service;
    }

    @GetMapping("/send_message")
    public String showMessageForm(@PathVariable Long serverId, @PathVariable Long channelId, @AuthenticationPrincipal OAuth2User principal, Model model) {
        String userId = principal.getName();
        Member member = accessService.getServerMember(userId, serverId);
        TextChannel channel = accessService.getWritableServerTextChannel(member, channelId);

        model.addAttribute("message", new DiscordMessage());
        model.addAttribute("channelName", channel.getName());

        return "/action/message";
    }

    @PostMapping("/send_message")
    public String sendMessageInChannel(@Validated @ModelAttribute("message") DiscordMessage message, BindingResult result, @PathVariable Long serverId, @PathVariable Long channelId, @AuthenticationPrincipal OAuth2User principal, Model model) {

        String userId = principal.getName();
        Member member = accessService.getServerMember(userId, serverId);
        TextChannel channel = accessService.getWritableServerTextChannel(member, channelId);

        if(result.hasErrors()) {
            model.addAttribute("message", message);
            model.addAttribute("channelName", channel.getName());
            return "/action/message";
        }

        service.sendMessageInChannel(member, channel, message.getContent());

        return "redirect:?success";
    }

}

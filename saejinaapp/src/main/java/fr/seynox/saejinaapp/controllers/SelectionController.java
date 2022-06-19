package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannel;
import fr.seynox.saejinaapp.services.DiscordService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class SelectionController {

    private final DiscordService service;

    public SelectionController(DiscordService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String showServerSelection(Model model, @AuthenticationPrincipal OAuth2User principal) {
        String userId = principal.getName();
        List<Server> serverList = service.getUserServers(userId);

        model.addAttribute("serverList", serverList);
        return "/selection/servers";
    }

    @GetMapping("/{serverId}")
    public String showChannelSelection(Model model, @PathVariable Long serverId, @AuthenticationPrincipal OAuth2User principal) {
        String userId = principal.getName();

        List<TextChannel> textChannels = service.getVisibleServerTextChannels(userId, serverId);
        model.addAttribute("channels", textChannels);
        model.addAttribute("serverId", serverId);

        return "/selection/channels";
    }

}

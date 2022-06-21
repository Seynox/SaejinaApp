package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.ServerNotAccessibleException;
import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.TextChannel;
import fr.seynox.saejinaapp.services.MemberAccessService;
import fr.seynox.saejinaapp.services.DiscordService;
import net.dv8tion.jda.api.entities.Member;
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
    private final MemberAccessService serverService;

    public SelectionController(DiscordService service, MemberAccessService serverService) {
        this.service = service;
        this.serverService = serverService;
    }

    /**
     * Show the server selection menu.
     * Only shows mutual servers
     * @param model The model used in the thymeleaf template
     * @param principal The logged-in user
     * @return The path to the Thymeleaf template
     */
    @GetMapping("/")
    public String showServerSelection(Model model, @AuthenticationPrincipal OAuth2User principal) {
        String userId = principal.getName();
        List<Server> serverList = service.getUserServers(userId);

        model.addAttribute("serverList", serverList);
        return "/selection/servers";
    }

    /**
     * Show to channel selection menu for the given server
     * @param model The model used in the thymeleaf template
     * @param serverId The server selected
     * @param principal The logged-in user
     * @throws ServerNotAccessibleException When the server is not accessible by the user/bot.
     * Handled by {@link ExceptionController#showServerNotAccessible(Exception, Model)}
     * @return The path to the Thymeleaf template
     */
    @GetMapping("/{serverId}")
    public String showChannelSelection(Model model, @PathVariable Long serverId, @AuthenticationPrincipal OAuth2User principal) {
        String userId = principal.getName();

        Member member = serverService.getServerMember(userId, serverId);
        List<TextChannel> textChannels = service.getVisibleServerTextChannels(member);

        model.addAttribute("channels", textChannels);
        model.addAttribute("serverId", serverId);

        return "/selection/channels";
    }

}

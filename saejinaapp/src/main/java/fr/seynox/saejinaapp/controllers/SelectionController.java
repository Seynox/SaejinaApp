package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import fr.seynox.saejinaapp.models.Server;
import fr.seynox.saejinaapp.models.DiscordTextChannel;
import fr.seynox.saejinaapp.models.TextChannelAction;
import fr.seynox.saejinaapp.services.MemberAccessService;
import fr.seynox.saejinaapp.services.DiscordService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
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
    private final MemberAccessService accessService;

    public SelectionController(DiscordService service, MemberAccessService accessService) {
        this.service = service;
        this.accessService = accessService;
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
     * Show the channel selection menu for the given server
     * @param model The model used in the thymeleaf template
     * @param serverId The server selected
     * @param principal The logged-in user
     * @throws ResourceNotAccessibleException When the resource is not accessible by the user/bot.
     * Handled by {@link ExceptionController#showSaejinaAppException(Exception, Model)}
     * @return The path to the Thymeleaf template
     */
    @GetMapping("/{serverId}")
    public String showChannelSelection(Model model, @PathVariable Long serverId, @AuthenticationPrincipal OAuth2User principal) {
        String userId = principal.getName();

        Member member = accessService.getServerMember(userId, serverId);
        List<DiscordTextChannel> discordTextChannels = service.getVisibleServerTextChannels(member);

        model.addAttribute("channels", discordTextChannels);

        return "/selection/channels";
    }

    /**
     * Show the action selection menu for the given channel
     * @param model The model used in the thymeleaf template
     * @param serverId The server selected
     * @param channelId The channel selected
     * @param principal The logged-in user
     * @throws ResourceNotAccessibleException When the resource is not accessible by the user/bot.
     * Handled by {@link ExceptionController#showSaejinaAppException(Exception, Model)}}
     * @return The path to the Thymeleaf template
     */
    @GetMapping("/{serverId}/{channelId}")
    public String showActionSelection(Model model, @PathVariable Long serverId, @PathVariable Long channelId, @AuthenticationPrincipal OAuth2User principal) {
        String userId = principal.getName();

        Member member = accessService.getServerMember(userId, serverId);
        TextChannel channel = accessService.getServerTextChannel(member, channelId);

        List<TextChannelAction> actions = service.getPossibleActionsForChannel(member, channel);
        model.addAttribute("actions", actions);

        return "/selection/actions";
    }

}

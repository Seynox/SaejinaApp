package fr.seynox.saejinaapp.services;

import fr.seynox.saejinaapp.exceptions.PermissionException;
import fr.seynox.saejinaapp.models.Selectable;
import fr.seynox.saejinaapp.models.SelectableImpl;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.springframework.stereotype.Service;

import java.util.List;

import static fr.seynox.saejinaapp.models.TextChannelAction.SEND_ROLE_BUTTON;

@Service
public class RoleService {

    public static final String ROLE_ASSIGNMENT_TEMPLATE = "role-assign#%s";

    /**
     * Get all the roles that the member can assign
     * @param member The member getting the roles
     * @return A list of assignable roles
     */
    public List<Selectable> getAssignableRolesForMember(Member member) {
        Guild guild = member.getGuild();

        return guild.getRoles().stream()
                .filter(member::canInteract)
                .map(role -> new SelectableImpl(role.getIdLong(), role.getName()))
                .map(Selectable.class::cast)
                .toList();
    }

    /**
     * Send a button giving the role to the member that clicked the button.
     * WARNING ! This does not check if the member has the permission to assign the given role
     * @param member The member sending the button
     * @param channel The channel to send the button to
     * @param role The role that the button should assign
     * @throws PermissionException If the member does not have the permission to send role buttons
     */
    public void sendRoleButtonInChannel(Member member, TextChannel channel, Role role) {
        boolean isAllowed = SEND_ROLE_BUTTON.isAllowed(member, channel);
        if(!isAllowed) {
            throw new PermissionException("You do not have the permission to send role buttons");
        }

        Long roleId = role.getIdLong();
        String roleButtonId = ROLE_ASSIGNMENT_TEMPLATE.formatted(roleId);
        String roleName = role.getName();
        Button roleButton = Button.of(ButtonStyle.SECONDARY, roleButtonId, roleName);

        channel.sendMessage("⌄")
                .setActionRow(roleButton)
                .queue();
    }

}

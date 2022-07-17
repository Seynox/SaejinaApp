package fr.seynox.saejinaapp.utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ButtonUtils {


    /**
     * Append the given button to the last message if it was sent by the bot.
     * Otherwise, sends a new message with the button attached to it.
     * This does not check if the channel is writable for the bot
     * @param channel The channel to send the button to
     * @param button The button to send/append
     */
    public void sendOrAppendButton(TextChannel channel, Button button) {
        long lastMessageId = channel.getLatestMessageIdLong();
        Message lastMessage = channel.retrieveMessageById(lastMessageId).complete();

        Long lastMessageAuthorId = lastMessage.getAuthor().getIdLong();
        Long botId = channel.getJDA().getSelfUser().getIdLong();
        boolean isLastMessageFromSelf = Objects.equals(botId, lastMessageAuthorId);

        String buttonId = Optional.ofNullable(button.getId()).orElse("");
        boolean alreadyHasButton = lastMessage.getButtonById(buttonId) != null;

        if(isLastMessageFromSelf && !alreadyHasButton) {
            appendButtonToMessage(button, lastMessage);
        } else {
            channel.sendMessage("** **")
                    .setActionRows(ActionRow.of(button))
                    .queue();
        }
    }

    /**
     * Adds the button to the given message.
     * This does not check if the message already has a button with the same id
     * @param button The button to append to the message
     * @param message A message sent by the bot
     */
    public void appendButtonToMessage(Button button, Message message) {
        List<ItemComponent> messageActionRows = message.getActionRows().stream()
                .flatMap(row -> row.getComponents().stream())
                .toList();

        List<ItemComponent> updatedActionRows = new ArrayList<>(messageActionRows);
        updatedActionRows.add(button);

        List<ActionRow> partitionedActionRows = ActionRow.partitionOf(updatedActionRows);
        message.editMessageComponents(partitionedActionRows).queue();
    }

}

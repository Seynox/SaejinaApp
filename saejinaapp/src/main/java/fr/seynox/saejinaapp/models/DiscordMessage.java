package fr.seynox.saejinaapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Message;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscordMessage {

    @NotBlank(message = "The message cannot be blank")
    @Size(min = 1, max = Message.MAX_CONTENT_LENGTH, message = "The message must be between 1 and 2000 characters")
    private String content;

}

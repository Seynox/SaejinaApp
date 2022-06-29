package fr.seynox.saejinaapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StringRequest {

    @NotBlank(message = "The content cannot be blank")
    @Size(min = 1, max = Message.MAX_CONTENT_LENGTH, message = "The message must be between 1 and 2000 characters", groups = Message.class)
    @Size(min = 1, max = Button.LABEL_MAX_LENGTH, message = "The label must be between 1 and 80 characters", groups = Button.class)
    private String content;

}

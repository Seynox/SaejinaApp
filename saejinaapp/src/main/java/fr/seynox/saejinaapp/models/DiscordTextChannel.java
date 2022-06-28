package fr.seynox.saejinaapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscordTextChannel implements Selectable {

    private Long id;
    private String name;

    @Override
    public String getId() {
        return String.valueOf(id);
    }
}

package fr.seynox.saejinaapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Server implements Selectable {

    private String name;
    private String iconUrl;
    private Long id;

}

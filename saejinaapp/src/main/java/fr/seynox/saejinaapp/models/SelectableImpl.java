package fr.seynox.saejinaapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectableImpl implements Selectable {

    private Object id;
    private String name;

}

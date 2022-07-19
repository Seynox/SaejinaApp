package fr.seynox.saejinaapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectableImpl implements Selectable {

    private Object id;
    private String iconUrl;
    private String name;

    public SelectableImpl(Object id, String name) {
        this.id = id;
        this.name = name;
    }

}

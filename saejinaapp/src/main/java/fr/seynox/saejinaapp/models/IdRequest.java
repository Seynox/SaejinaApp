package fr.seynox.saejinaapp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdRequest {

    @NotNull(message = "The id cannot be null")
    private Long id;

}

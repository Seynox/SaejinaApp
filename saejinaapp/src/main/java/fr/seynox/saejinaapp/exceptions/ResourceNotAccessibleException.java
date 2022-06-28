package fr.seynox.saejinaapp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotAccessibleException extends SaejinaAppException {

    private static final String DEFAULT_MESSAGE = "The resource does not exist, or is not accessible for the user";

    public ResourceNotAccessibleException() {
        this(DEFAULT_MESSAGE);
    }

    public ResourceNotAccessibleException(String message) {
        super(message);
    }

}

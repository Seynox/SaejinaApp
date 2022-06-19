package fr.seynox.saejinaapp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ServerNotAccessibleException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Server does not exist, or is not accessible for the given user";

    public ServerNotAccessibleException() {
        this(DEFAULT_MESSAGE);
    }

    public ServerNotAccessibleException(String message) {
        super(message);
    }

}

package fr.seynox.saejinaapp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PermissionException extends SaejinaAppException {

    private static final String DEFAULT_MESSAGE = "You do not have the permissions required to do that";

    public PermissionException() {
        this(DEFAULT_MESSAGE);
    }

    public PermissionException(String message) {
        super(message);
    }

}

package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import fr.seynox.saejinaapp.exceptions.SaejinaAppException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * The class handling exceptions thrown in controllers
 */
@ControllerAdvice
public class ExceptionController {

    private static final String ERROR_TEMPLATE_PATH = "error";
    private static final String ERROR_MESSAGE_ATTRIBUTE = "message";

    /**
     * Handles {@link ResourceNotAccessibleException}
     * @return The path to the Thymeleaf template
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SaejinaAppException.class)
    public String showSaejinaAppException(Exception exception, Model model) {

        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, exception.getMessage());
        return ERROR_TEMPLATE_PATH;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String send404PageOnBadRequest() {
        return "error/404";
    }

}

package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import fr.seynox.saejinaapp.exceptions.SaejinaAppException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * The class handling exceptions thrown in controllers
 */
@ControllerAdvice
public class ExceptionController {

    private static final String ERROR_TEMPLATE_PATH = "/exception/error";
    private static final String ERROR_MESSAGE_ATTRIBUTE = "message";

    private static final Logger LOGGER = LogManager.getLogger(ExceptionController.class);

    /**
     * Handles {@link ResourceNotAccessibleException}
     * @return The path to the Thymeleaf template
     */
    @ExceptionHandler(SaejinaAppException.class)
    public String showSaejinaAppException(Exception exception, Model model) {

        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, exception.getMessage());
        return ERROR_TEMPLATE_PATH;
    }

    /**
     * The exception fallback
     * @return The path to the Thymeleaf template
     */
    @ExceptionHandler(Exception.class)
    public String showErrorPage(Exception exception, Model model) {
        LOGGER.debug("An error occurred", exception);

        model.addAttribute(ERROR_MESSAGE_ATTRIBUTE, "An error occurred");
        return ERROR_TEMPLATE_PATH;
    }

}

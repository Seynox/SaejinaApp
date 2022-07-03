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

    private static final Logger LOGGER = LogManager.getLogger(ExceptionController.class);

    /**
     * Handles {@link ResourceNotAccessibleException}
     * @return The path to the Thymeleaf template
     */
    @ExceptionHandler({SaejinaAppException.class})
    public String showSaejinaAppException(Exception exception, Model model) {

        model.addAttribute("message", exception.getMessage());
        return "/exception/error";
    }

    /**
     * The exception fallback
     * @return The path to the Thymeleaf template
     */
    @ExceptionHandler(Exception.class)
    public String showErrorPage(Exception exception, Model model) {
        LOGGER.debug("An error occurred", exception);

        model.addAttribute("message", "An error occurred");
        return "/exception/error";
    }

}

package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.ResourceNotAccessibleException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionController {

    /**
     * Handles {@link ResourceNotAccessibleException}
     * @return The path to the Thymeleaf template
     */
    @ExceptionHandler(ResourceNotAccessibleException.class)
    public String showResourceNotAccessible(Exception exception, Model model) {

        model.addAttribute("message", exception.getMessage());
        return "/exception/resource_access";
    }

    /**
     * The exception fallback
     * @return The path to the Thymeleaf template
     */
    @ExceptionHandler(Exception.class)
    public String showErrorPage() {
        return "/exception/error";
    }

}

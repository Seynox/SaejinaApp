package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.ServerNotAccessibleException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionController {

    /**
     * Handle the ServerNotAccessible exception
     * @return The path to the Thymeleaf template
     */
    @ExceptionHandler(ServerNotAccessibleException.class)
    public String showServerNotAccessible(Exception exception, Model model) {

        model.addAttribute("message", exception.getMessage());
        return "/exception/server_access";
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

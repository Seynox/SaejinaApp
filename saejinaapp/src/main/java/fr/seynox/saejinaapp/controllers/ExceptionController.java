package fr.seynox.saejinaapp.controllers;

import fr.seynox.saejinaapp.exceptions.ServerNotAccessibleException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionController {

    @ExceptionHandler(ServerNotAccessibleException.class)
    public String showServerNotAccessible(Exception exception, Model model) {

        model.addAttribute("message", exception.getMessage());
        return "/exception/server_access";
    }

    @ExceptionHandler(Exception.class)
    public String showErrorPage() {
        return "/exception/error";
    }

}

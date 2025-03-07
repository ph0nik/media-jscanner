package app.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import service.exceptions.ConfigurationException;
import service.exceptions.NetworkException;
import service.exceptions.NoApiKeyException;
import service.exceptions.NoQueryFoundException;

import java.io.FileNotFoundException;

@ControllerAdvice
public class ExceptionController {

    @ExceptionHandler(value = {
            NoQueryFoundException.class,
            FileNotFoundException.class,
            NetworkException.class,
            ConfigurationException.class,
            NoApiKeyException.class
    })
    // TODO  Exception evaluating SpringEL expression: "query_list_movie.size()>0" (template: "error" - line 30, col 31)
    // query list is null when showing error
    protected String getErrorMessage(Exception ex, Model model) {
        model.addAttribute("error_msg", ex.getMessage());
        return "error";
    }

}

package app.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import service.exceptions.*;

import java.io.FileNotFoundException;

@ControllerAdvice
public class ExceptionController {

    @ExceptionHandler(value = {
            NoQueryFoundException.class,
            FileNotFoundException.class,
            NetworkException.class,
            ConfigurationException.class,
            NoApiKeyException.class,
            MissingReferenceMediaQueryException.class
//            Exception.class // temp
    })
    protected String getErrorMessage(Exception ex, Model model) {
        model.addAttribute("error_details", ex.getMessage());
        model.addAttribute("error_string", ex.toString());
        return "error";
    }




}

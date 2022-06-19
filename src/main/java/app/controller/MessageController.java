package app.controller;

import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class MessageController {

    @SendTo("/message")
    public String sendMessage() {
        return "none";
    }
}

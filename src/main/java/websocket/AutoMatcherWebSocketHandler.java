package websocket;

import org.springframework.context.event.EventListener;
import org.springframework.web.socket.handler.TextWebSocketHandler;


public class AutoMatcherWebSocketHandler extends TextWebSocketHandler {

    @EventListener
    void checkForChange(EventListener eventListener) {

        System.out.println(eventListener.id());
    }


}


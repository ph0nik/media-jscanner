package websocket.config;

import model.AutoMatcherStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationsController {

    private final static Logger LOG = LoggerFactory.getLogger(NotificationsController.class);

    private final NotificationDispatcher dispatcher;

    @Autowired
    public NotificationsController(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @MessageMapping("/start")
    public void start(StompHeaderAccessor stompHeaderAccessor) {
        LOG.info("[ dispatcher ] registered client: {}", stompHeaderAccessor.getSessionId());
        dispatcher.add(stompHeaderAccessor.getSessionId());
    }

    @MessageMapping("/stop")
    public void stop(StompHeaderAccessor stompHeaderAccessor) {
        LOG.info("[ dispatcher ] client unregistered: {}", stompHeaderAccessor.getSessionId());
        dispatcher.remove(stompHeaderAccessor.getSessionId());
    }

    @MessageMapping("/runauto")
    public void auto(StompHeaderAccessor stompHeaderAccessor) {
//        System.out.println(stompHeaderAccessor.getMessage());
        AutoMatcherStatus status = new AutoMatcherStatus();
        status.setEnabled(true);
        status.setTotalElements(10);
        status.setCurrentElementNumber(3);
        status.setCurrentFile("d:\\folder\\folder\\plik.mkv");
        dispatcher.dispatch(status);
    }
}
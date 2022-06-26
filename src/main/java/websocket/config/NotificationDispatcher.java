package websocket.config;

import model.AutoMatcherStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashSet;
import java.util.Set;

@Component
public class NotificationDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final String NOTIFICATION_DEST = "/notification/item";

    private final SimpMessagingTemplate template;

    private Set<String> listeners = new HashSet<>();

    public NotificationDispatcher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void add(String sessionId) {
        listeners.add(sessionId);
    }

    public void remove(String sessionId) {
        listeners.remove(sessionId);
    }

    public void dispatch() {
        for (String listener : listeners) {
            LOG.info("Send message to user: {}", listener);
            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headerAccessor.setSessionId(listener);
            headerAccessor.setLeaveMutable(true);

            template.convertAndSendToUser(listener,
                    NOTIFICATION_DEST, "check this", headerAccessor.getMessageHeaders());
        }
    }

    public void dispatch(AutoMatcherStatus status) {
        for (String listener : listeners) {
            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headerAccessor.setSessionId(listener);
            headerAccessor.setLeaveMutable(true);
            LOG.info("Dispatcher message: {}", status);
            template.convertAndSendToUser(listener,
                    NOTIFICATION_DEST, status, headerAccessor.getMessageHeaders());
        }
    }

    public void sessionDisconectionHandler(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        LOG.info("Disconnecting {} !", sessionId);
        remove(sessionId);
    }
}

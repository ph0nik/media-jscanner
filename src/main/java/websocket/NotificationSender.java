package websocket;

public abstract class NotificationSender<T> {

    protected abstract void sendNotification(T notification);
}

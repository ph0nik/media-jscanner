package service;

import model.LinkCreationResult;
import org.springframework.stereotype.Component;

@Component
public class ErrorNotificationService {

    private LinkCreationResult linkCreationResult;

    public void setLinkCreationResult(LinkCreationResult linkCreationResult) {
        this.linkCreationResult = linkCreationResult;
    }

    public String getCurrentResult() {
        if (linkCreationResult == null || linkCreationResult.isCreationStatus()) return "OK";
        return linkCreationResult.getCreationMessage();
    }
}

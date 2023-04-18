package service;

import model.OperationResult;
import org.springframework.stereotype.Component;

@Component
public class ErrorNotificationService {

    private OperationResult operationResult;

    public void setLinkCreationResult(OperationResult operationResult) {
        this.operationResult = operationResult;
    }

    public String getCurrentResult() {
        if (operationResult == null || operationResult.isCreationStatus()) return "OK";
        return operationResult.getCreationMessage();
    }
}

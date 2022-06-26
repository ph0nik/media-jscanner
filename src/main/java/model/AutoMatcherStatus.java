package model;

import websocket.DefaultNotification;

public class AutoMatcherStatus extends DefaultNotification<AutoMatcherStatus> {

    private boolean enabled;
    private String currentFile;
    private int totalElements;
    private int currentElementNumber;

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getCurrentElementNumber() {
        return currentElementNumber;
    }

    public void setCurrentElementNumber(int currentElementNumber) {
        this.currentElementNumber = currentElementNumber;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "AutoMatcherStatus{" +
                "enabled=" + enabled +
                ", currentFile='" + currentFile + '\'' +
                ", totalElements=" + totalElements +
                ", currentElementNumber=" + currentElementNumber +
                '}';
    }
}

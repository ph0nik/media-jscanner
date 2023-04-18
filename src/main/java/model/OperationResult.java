package model;

public class OperationResult {

    private boolean creationStatus;
    private String creationMessage;
    private MediaLink mediaLink;

    public OperationResult(boolean creationStatus, String creationMessage, MediaLink mediaLink) {
        this(creationStatus, creationMessage);
        this.mediaLink = mediaLink;
    }

    public OperationResult(boolean creationStatus, String creationMessage) {
        this.creationMessage = creationMessage;
        this.creationStatus = creationStatus;
    }

    public String getCreationMessage() {
        return creationMessage;
    }

    public void setCreationMessage(String creationMessage) {
        this.creationMessage = creationMessage;
    }

    public boolean isCreationStatus() {
        return creationStatus;
    }

    public void setCreationStatus(boolean creationStatus) {
        this.creationStatus = creationStatus;
    }

    public MediaLink getMediaLink() {
        return mediaLink;
    }

    public void setMediaLink(MediaLink mediaLink) {
        this.mediaLink = mediaLink;
    }

    @Override
    public String toString() {
        return "SymLinkCreationResult{" +
                "creationStatus=" + creationStatus +
                ", creationMessage='" + creationMessage + '\'' +
                ", mediaLink=" + mediaLink +
                '}';
    }
}

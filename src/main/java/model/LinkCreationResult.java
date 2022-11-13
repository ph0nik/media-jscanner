package model;

public class LinkCreationResult {

    private boolean creationStatus;
    private String creationMessage;
    private MediaLink mediaLink;

    public LinkCreationResult(boolean creationStatus, String creationMessage, MediaLink mediaLink) {
        this.creationMessage = creationMessage;
        this.creationStatus = creationStatus;
        this.mediaLink = mediaLink;
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

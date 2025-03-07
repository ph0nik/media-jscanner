package service.backup.model;

import model.MediaLink;

import java.util.List;

public class BackupXml {

    private List<MediaLink> mediaLinks;

    public List<MediaLink> getMediaLinks() {
        return mediaLinks;
    }

    public void setMediaLinks(List<MediaLink> mediaLinks) {
        this.mediaLinks = mediaLinks;
    }
}

package model.links;

import model.MediaLink;

import java.util.Objects;

public class TvMediaLinkDto {
    MediaLink mediaLink;
    int episodeNumber;

    public TvMediaLinkDto() {}

    public TvMediaLinkDto(MediaLink mediaLink, int episodeNumber) {
        this.mediaLink = mediaLink;
        this.episodeNumber = episodeNumber;
    }


    public MediaLink getMediaLink() {
        return mediaLink;
    }

    public void setMediaLink(MediaLink mediaLink) {
        this.mediaLink = mediaLink;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TvMediaLinkDto that = (TvMediaLinkDto) o;
        return episodeNumber == that.episodeNumber && mediaLink.equals(that.mediaLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaLink, episodeNumber);
    }

    @Override
    public String toString() {
        return "TvMediaLinkDto{" +
                "mediaLink=" + mediaLink +
                ", episodeNumber=" + episodeNumber +
                '}';
    }
}

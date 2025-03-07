package model.links;

import java.util.LinkedList;
import java.util.List;

public class MediaLinkDto {

    List<TvMediaLinkDto> mediaLinkDtos;

    public MediaLinkDto() {
        mediaLinkDtos = new LinkedList<>();
    }

    public void addMediaLinkDto(TvMediaLinkDto tvMediaLinkDto) {
        mediaLinkDtos.add(tvMediaLinkDto);
    }

    public List<TvMediaLinkDto> getMediaLinkDtos() {
        return mediaLinkDtos;
    }

    public void setMediaLinkDtos(List<TvMediaLinkDto> mediaLinkDtos) {
        this.mediaLinkDtos = mediaLinkDtos;
    }

    @Override
    public String toString() {
        return "MediaLinkDto{" +
                "mediaLinkDtos=" + mediaLinkDtos +
                '}';
    }
}

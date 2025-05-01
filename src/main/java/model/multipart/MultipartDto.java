package model.multipart;

import model.MediaQuery;
import util.MediaType;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MultipartDto {
    private UUID queryUuid;
    private List<MultiPartElement> multiPartElementList;

    public MultipartDto() {
        multiPartElementList = new LinkedList<>();
    }

    public List<MultiPartElement> getMultiPartElementList() {
        return multiPartElementList;
    }

    public void setMultiPartElementList(List<MultiPartElement> multiPartElementList) {
        this.multiPartElementList = multiPartElementList;
    }

    public void addMultiPartElement(
            MediaQuery mediaQuery,
            int counter,
            MediaType mediaType) {
        multiPartElementList.add(new MultiPartElement(mediaQuery, counter, mediaType));
    }

    public UUID getQueryUuid() {
        return queryUuid;
    }

    public void setQueryUuid(UUID queryUuid) {
        this.queryUuid = queryUuid;
    }

    @Override
    public String toString() {
        return "MultipartDto{" +
                "currentQuery=" + queryUuid +
                ", multiPartElementList=" + multiPartElementList +
                '}';
    }
}

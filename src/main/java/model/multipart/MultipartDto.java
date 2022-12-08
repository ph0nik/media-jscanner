package model.multipart;

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

    public void addMultiPartElement(MultiPartElement multiPartElement) {
        multiPartElementList.add(multiPartElement);
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

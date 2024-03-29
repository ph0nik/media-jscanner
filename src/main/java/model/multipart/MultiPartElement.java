package model.multipart;

import util.MediaType;

public class MultiPartElement {

    private String filePath;
    private boolean multipartSwitch;
    private byte partNumber;
    private MediaType mediaType;

    public MultiPartElement() {
        partNumber = 0;
        multipartSwitch = false;
    }

    public boolean getMultipartSwitch() {
        return multipartSwitch;
    }

    public void setMultipartSwitch(boolean multipartSwitch) {
        this.multipartSwitch = multipartSwitch;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public byte getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(byte partNumber) {
        this.partNumber = partNumber;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public String toString() {
        return "MultiPartElement{" +
                "filePath='" + filePath + '\'' +
                ", multipartSwitch=" + multipartSwitch +
                ", multiPart=" + partNumber +
                ", mediaType=" + mediaType +
                '}';
    }
}

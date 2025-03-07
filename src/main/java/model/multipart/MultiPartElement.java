package model.multipart;

import util.MediaType;

public class MultiPartElement {

    private String filePath;
    private String fileName;
    private boolean multipartSwitch;
    private byte partNumber;
    private MediaType mediaType;

    public MultiPartElement() {
        partNumber = 0;
        multipartSwitch = false;
    }

    public MultiPartElement(String mediaQueryFilePath) {
        this.filePath = mediaQueryFilePath;
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

    public String getFileName() {
        int x;
        if (filePath.contains("\\")) {
            x = filePath.lastIndexOf('\\');
        } else {
            x = filePath.lastIndexOf("/");
        }
        return filePath.substring(x, filePath.length());
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public boolean isMultipartSwitch() {
        return multipartSwitch;
    }

    @Override
    public String toString() {
        return "MultiPartElement{" +
                "filePath='" + filePath + '\'' +
                ", multipartSwitch=" + multipartSwitch +
                ", partNumber=" + partNumber +
                ", mediaType=" + mediaType +
                '}';
    }
}

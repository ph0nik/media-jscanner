package model.multipart;

import model.MediaQuery;
import util.MediaType;

public class MultiPartElement {

    private String filePath;
    private String fileName;
    private boolean multipartSwitch = true;
    private int partNumber = 0;
    private MediaType mediaType;

    public MultiPartElement() {}

    public MultiPartElement(String mediaQueryFilePath) {
        this.filePath = mediaQueryFilePath;
    }

    public MultiPartElement(MediaQuery mediaQuery, int partNumber, MediaType mediaType) {
        this.mediaType = mediaType;
        this.filePath = mediaQuery.getFilePath();
        this.partNumber = partNumber;
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
        return filePath.substring(x);
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
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

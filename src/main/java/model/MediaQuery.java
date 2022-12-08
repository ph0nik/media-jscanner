package model;


import util.MediaType;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class MediaQuery {

    private long queryId;
    private UUID queryUuid;
    private String filePath;
    /*
    * -1 not multipart but grouped
    * 0 default
    * +1 multipart and grouped
    * */
    private byte multipart;
    private MediaType mediaType;

    public MediaQuery(String filePath) {
        this.filePath = filePath;
        multipart = -1;
        queryUuid = UUID.randomUUID();
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public byte getMultipart() {
        return multipart;
    }

    public void setMultipart(byte multipart) {
        this.multipart = multipart;
    }

    public String getFileName() {
        Path of = Path.of(filePath);
        return of.getName(of.getNameCount() - 1).toString();
    }

    public String getParentPath() {
        Path of = Path.of(filePath);
        return of.getParent().toString();
    }

    public UUID getQueryUuid() {
        return queryUuid;
    }

    public void setQueryUuid(UUID queryUuid) {
        this.queryUuid = queryUuid;
    }

    public long getQueryId() {
        return queryId;
    }

    public void setQueryId(long queryId) {
        this.queryId = queryId;
    }

    public String getQuery() {
        Path path = Path.of(filePath);
        String fileName = path.getName(path.getNameCount() - 1).toString();
        String sub = fileName.substring(0, fileName.lastIndexOf("."));
        return sub.replace(".", " ");
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaQuery that = (MediaQuery) o;
        return filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath);
    }

    @Override
    public String toString() {
        return "MediaQuery{" +
                "queryId=" + queryId +
                ", queryUuid=" + queryUuid +
                ", filePath='" + filePath + '\'' +
                ", multipart=" + multipart +
                ", mediaType=" + mediaType +
                '}';
    }
}

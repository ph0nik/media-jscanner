package model;


import util.MediaType;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class MediaQuery {
    private long queryId;
    private UUID queryUuid;
    private String filePath;
    private int multipart;
    private MediaType mediaType;

    public MediaQuery(String filePath, MediaType mediaType) {
        this.filePath = filePath;
        this.mediaType = mediaType;
        multipart = -1;
        queryUuid = UUID.randomUUID();
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public int getMultipart() {
        return multipart;
    }

    /*
     *TODO
     * -1 for grouped element but not part of reference media
     * 0 default
     * +1 multipart and grouped
     * */
    public void setMultipart(int multipart) {
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
        String fileName = Path.of(filePath).getFileName().toString();
//        String fileName = path.getName(path.getNameCount() - 1).toString(); TODO
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
        return multipart == that.multipart && queryUuid.equals(that.queryUuid) && filePath.equals(that.filePath) && mediaType == that.mediaType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryUuid, filePath, multipart, mediaType);
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

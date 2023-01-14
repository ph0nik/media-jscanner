package model.form;

import util.MediaType;

import java.util.UUID;

public class WebSearchResultForm {

    private Long id;
    private MediaType mediaType;
    private String title;
    private String originalPath;
    private int theMovieDbId;
    private String imdbId;
    private String description;
    private String url;
    private UUID queryId;

    public UUID getQueryId() {
        return queryId;
    }

    public void setQueryId(UUID queryId) {
        this.queryId = queryId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public int getTheMovieDbId() {
        return theMovieDbId;
    }

    public void setTheMovieDbId(int theMovieDbId) {
        this.theMovieDbId = theMovieDbId;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "WebSearchResultForm{" +
                "id=" + id +
                ", mediaType=" + mediaType +
                ", title='" + title + '\'' +
                ", originalPath='" + originalPath + '\'' +
                ", theMovieDbId=" + theMovieDbId +
                ", imdbId='" + imdbId + '\'' +
                ", description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", queryId=" + queryId +
                '}';
    }
}

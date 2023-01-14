package model;

import util.MediaType;

import java.util.Objects;
import java.util.UUID;

public class QueryResult implements Comparable<QueryResult> {

    private Long id;
    private MediaType mediaType;
    private String title;
    private String originalPath;
    private int theMovieDbId;
    private String imdbId;
    private String description;
    private String url;
    private UUID queryId;
    private String poster;
    private String year;

    public UUID getQueryId() {
        return queryId;
    }

    public void setQueryId(UUID queryId) {
        this.queryId = queryId;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTheMovieDbId() {
        return theMovieDbId;
    }

    public void setTheMovieDbId(int theMovieDbId) {
        this.theMovieDbId = theMovieDbId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "id=" + id +
                ", mediaType=" + mediaType +
                ", title='" + title + '\'' +
                ", originalPath='" + originalPath + '\'' +
                ", theMovieDbId=" + theMovieDbId +
                ", imdbId='" + imdbId + '\'' +
                ", description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", queryId=" + queryId +
                ", poster='" + poster + '\'' +
                ", year='" + year + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryResult that = (QueryResult) o;
        return theMovieDbId == that.theMovieDbId && url.equals(that.url) && imdbId.equals(that.imdbId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, theMovieDbId, imdbId);
    }

    @Override
    public int compareTo(QueryResult o) {
        int i = this.getTheMovieDbId() - o.getTheMovieDbId();
        if (i != 0) return i;
        int j = this.getImdbId().compareTo(o.getImdbId());
        if (j != 0) return j;
        return this.getUrl().compareTo(o.getUrl());
    }
}

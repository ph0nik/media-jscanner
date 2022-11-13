package model;

import java.util.Objects;

public class QueryResult implements Comparable<QueryResult> {

    private long id;
    private String url;
    private int theMovieDbId;
    private String imdbId;
    private String title;
    private String description;
    private String poster;
    private String year;
    private String originalPath;

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

    public long getId() {
        return id;
    }

    public void setId(long id) {
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
                ", url='" + url + '\'' +
                ", theMovieDbId=" + theMovieDbId +
                ", imdbId='" + imdbId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", poster='" + poster + '\'' +
                ", year='" + year + '\'' +
                ", originalPath='" + originalPath + '\'' +
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

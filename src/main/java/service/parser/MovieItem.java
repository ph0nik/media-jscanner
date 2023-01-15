package service.parser;

import com.google.gson.annotations.SerializedName;

public class MovieItem {
    @SerializedName(value = "id")
    private int id;
    @SerializedName(value = "title")
    private String title;
    @SerializedName(value = "overview")
    private String description;
    @SerializedName(value = "release_date")
    private String date;
    @SerializedName(value = "poster_path")
    private String poster;
    @SerializedName(value = "imdb_id")
    private String imdbId;

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "MovieItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", date='" + date + '\'' +
                ", poster='" + poster + '\'' +
                ", imdbId='" + imdbId + '\'' +
                '}';
    }
}

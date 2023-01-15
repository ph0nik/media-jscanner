package service.parser;

import com.google.gson.annotations.SerializedName;

public class TvItem {
    @SerializedName(value = "id")
    private int id;
    @SerializedName(value = "name")
    private String title;
    @SerializedName(value = "overview")
    private String description;
    @SerializedName(value = "first_air_date")
    private String date;
    @SerializedName(value = "poster_path")
    private String poster;

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
        return "TvItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", date='" + date + '\'' +
                ", poster='" + poster + '\'' +
                '}';
    }
}

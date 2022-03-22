package model;

public class MediaData {

    private String title;
    private int year;
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

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "MediaData{" +
                "title='" + title + '\'' +
                ", year=" + year +
                ", imdbId='" + imdbId + '\'' +
                '}';
    }
}

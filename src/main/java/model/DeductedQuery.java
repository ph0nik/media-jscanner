package model;

public class DeductedQuery {

    private String phrase;
    private String year;
    private String path;

    public DeductedQuery(String phrase, String year, String path) {
        this.phrase = phrase;
        this.year = year;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "DeductedQuery{" +
                "phrase='" + phrase + '\'' +
                ", year='" + year + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}

package service.parser;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MovieResults {

    @SerializedName(value = "page")
    private int page;
    @SerializedName(value = "results")
    private List<MovieItem> movieResults;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<MovieItem> getMovieResults() {
        return movieResults;
    }

    public void setMovieResults(List<MovieItem> movieResults) {
        this.movieResults = movieResults;
    }

    @Override
    public String toString() {
        return "MovieResults{" +
                "page=" + page +
                ", movieResults=" + movieResults +
                '}';
    }
}

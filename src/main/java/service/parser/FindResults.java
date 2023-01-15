package service.parser;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FindResults {
    @SerializedName(value = "movie_results")
    private List<MovieItem> movieResults;

    @SerializedName(value = "tv_results")
    private List<TvItem> tvResults;

    public List<TvItem> getTvResults() {
        return tvResults;
    }

    public void setTvResults(List<TvItem> tvResults) {
        this.tvResults = tvResults;
    }

    public List<MovieItem> getMovieResults() {
        return movieResults;
    }

    public void setMovieResults(List<MovieItem> movieResults) {
        this.movieResults = movieResults;
    }

    @Override
    public String toString() {
        return "FindResults{" +
                "movieResults=" + movieResults +
                ", tvResults=" + tvResults +
                '}';
    }
}

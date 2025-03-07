package service.parser;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TvResults {

    @SerializedName(value = "page")
    private int page;

    @SerializedName("results")
    private List<TvItem> results;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<TvItem> getResults() {
        return results;
    }

    public void setResults(List<TvItem> results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "TvResults{" +
                "page=" + page +
                ", results=" + results +
                '}';
    }
}

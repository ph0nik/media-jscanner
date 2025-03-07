package service.parser;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TvSeason {
    @SerializedName("episodes")
    private List<TvEpisode> episodes;

    public List<TvEpisode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<TvEpisode> episodes) {
        this.episodes = episodes;
    }

    @Override
    public String toString() {
        return "TvSeason{" +
                "episodes=" + episodes +
                '}';
    }
}

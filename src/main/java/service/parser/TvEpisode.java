package service.parser;

import com.google.gson.annotations.SerializedName;

public class TvEpisode {
    @SerializedName("episode_number")
    private int episodeNumber;

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    @Override
    public String toString() {
        return "TvEpisode{" +
                "episodeNumber=" + episodeNumber +
                '}';
    }
}

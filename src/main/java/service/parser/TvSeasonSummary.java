package service.parser;

import com.google.gson.annotations.SerializedName;

public class TvSeasonSummary {
    @SerializedName(value = "name")
    private String name;
    @SerializedName(value = "season_number")
    private int seasonNumber;
    @SerializedName(value = "episode_count")
    private int episodeCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(int seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }
}

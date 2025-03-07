package service.parser;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TvDetails {
    @SerializedName(value = "original_name")
    private String name;
    @SerializedName(value = "seasons")
    private List<TvSeasonSummary> seasonList;
    @SerializedName(value = "id")
    private int theMovieDbId;
    @SerializedName(value = "first_air_date")
    private String firstAirDate;
    @SerializedName(value = "external_ids")
    private TvExternalIds tvExternalIds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TvSeasonSummary> getSeasonList() {
        return seasonList;
    }

    public void setSeasonList(List<TvSeasonSummary> seasonList) {
        this.seasonList = seasonList;
    }

    public int getTheMovieDbId() {
        return theMovieDbId;
    }

    public void setTheMovieDbId(int theMovieDbId) {
        this.theMovieDbId = theMovieDbId;
    }

    public String getFirstAirDate() {
        return firstAirDate;
    }

    public void setFirstAirDate(String firstAirDate) {
        this.firstAirDate = firstAirDate;
    }

    public TvExternalIds getTvExternalIds() {
        return tvExternalIds;
    }

    public void setTvExternalIds(TvExternalIds tvExternalIds) {
        this.tvExternalIds = tvExternalIds;
    }
}

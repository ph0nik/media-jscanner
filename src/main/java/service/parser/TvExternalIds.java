package service.parser;

import com.google.gson.annotations.SerializedName;

public class TvExternalIds {
    @SerializedName(value = "imdb_id")
    private String imdbId;

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }
}

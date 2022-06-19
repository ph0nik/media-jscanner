package model;

import java.util.List;

public class LastRequest {
    private List<QueryResult> lastRequest;
    private MediaQuery lastMediaQuery;

    public LastRequest(List<QueryResult> queryResults, MediaQuery lastSelectedId) {
        this.lastMediaQuery = lastSelectedId;
        this.lastRequest = queryResults;
    }

    public List<QueryResult> getLastRequest() {
        return lastRequest;
    }

    public void setLastRequest(List<QueryResult> lastRequest) {
        this.lastRequest = lastRequest;
    }

    public MediaQuery getLastMediaQuery() {
        return lastMediaQuery;
    }

    public void setLastMediaQuery(MediaQuery lastMediaQuery) {
        this.lastMediaQuery = lastMediaQuery;
    }
}

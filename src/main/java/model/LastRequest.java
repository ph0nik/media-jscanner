package model;

import java.util.List;

public class LastRequest {
    private List<QueryResult> lastRequest;
    private long lastId;

    public LastRequest(List<QueryResult> queryResults, long lastSelectedId) {
        this.lastId = lastSelectedId;
        this.lastRequest = queryResults;
    }

    public List<QueryResult> getLastRequest() {
        return lastRequest;
    }

    public void setLastRequest(List<QueryResult> lastRequest) {
        this.lastRequest = lastRequest;
    }

    public long getLastId() {
        return lastId;
    }

    public void setLastId(long lastId) {
        this.lastId = lastId;
    }
}

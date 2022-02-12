package main.model;

import java.util.Objects;

public class Query {

    private long queryId;
    private String query;
    private String fileName;

    public Query(String query, String fileName) {
        this.query = query;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Query query1 = (Query) o;
        return query.equals(query1.query) && fileName.equals(query1.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, fileName);
    }
}

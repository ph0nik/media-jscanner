package main.dao;

import main.model.Query;

public interface MediaTrackerDao {

    void addQueryToQueue(Query query);
}

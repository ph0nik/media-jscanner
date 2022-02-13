package dao;

import model.MediaLInk;
import model.MediaQuery;

public interface MediaTrackerDao {

    void addQueryToQueue(MediaQuery query);

    void removeQueryFromQueue(MediaQuery query);

    MediaQuery findQueryByName(String path);

    void addNewLink(MediaLInk mediaLInk);

    void removeLink(MediaLInk mediaLInk);




}

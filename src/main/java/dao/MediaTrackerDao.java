package dao;

import model.MediaLink;
import model.MediaQuery;

import java.util.List;

public interface MediaTrackerDao {

    void addQueryToQueue(MediaQuery query);

    void removeQueryFromQueue(MediaQuery query);

    MediaQuery findQueryByName(String path);

    List<MediaQuery> getAllMediaQueries();

    void addNewLink(MediaLink mediaLInk);

    void removeLink(MediaLink mediaLInk);

    List<MediaLink> getAllMediaLinks();

    MediaLink findMediaLinkByName(String name);




}

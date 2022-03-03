package dao;

import model.MediaLink;
import model.MediaQuery;

import java.util.List;

public interface MediaTrackerDao {

    void addQueryToQueue(MediaQuery query);

    void removeQueryFromQueue(MediaQuery query);

    MediaQuery findQueryByFilePath(String filePath);

    List<MediaQuery> getAllMediaQueries();

    List<MediaQuery> findQueryByParentPath(String filePath);

    void addNewLink(MediaLink mediaLInk);

    void removeLink(MediaLink mediaLInk);

    List<MediaLink> getAllMediaLinks();

    List<MediaLink> findMediaLinkByParentPath(String filePath);

    MediaLink findMediaLinkByFilePath(String filePath);




}

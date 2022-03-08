package dao;

import model.MediaLink;
import model.MediaQuery;

import java.util.List;

public interface MediaTrackerDao {

    /*
    * Add query element to queue
    * */
    void addQueryToQueue(MediaQuery query);

    /*
    * Remove query element from queue
    * */
    void removeQueryFromQueue(MediaQuery query);

    MediaQuery findQueryByFilePath(String filePath);

    /*
    * Get all media queries stored in database
    * */
    List<MediaQuery> getAllMediaQueries();

    /*
    * Return all MediaQuery elements that have given phrase within their filepath
    * */
    List<MediaQuery> findInFilePathQuery(String phrase);

    /*
    * Add new MediaLink element to database
    * */
    void addNewLink(MediaLink mediaLInk);

    /*
    * Remove MediaLink element from database
    * */
    void removeLink(MediaLink mediaLInk);

    /*
    * Return all MediaLink elements from database
    * */
    List<MediaLink> getAllMediaLinks();

    /*
     * Return all MediaLink elements that have given phrase within their filepath
     * */
    public List<MediaLink> findInFilePathLink(String phrase);

    /*
    * Return all MediaLink elements with given filepath
    * */
    MediaLink findMediaLinkByFilePath(String filePath);




}

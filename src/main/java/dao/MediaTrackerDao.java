package dao;

import model.MediaLink;

import java.util.List;

public interface MediaTrackerDao {

    /*
    * Add query element to queue
    * */
//    void addQueryToQueue(MediaQuery query);

    /*
    * Remove query element from queue
    * */
//    void removeQueryFromQueue(MediaQuery query);

//    MediaQuery findQueryByFilePath(String filePath);

//    MediaQuery getQueryById(Long id);

    /*
    * Get all media queries stored in database
    * */
//    List<MediaQuery> getAllMediaQueries();

    /*
    * Return all MediaQuery elements that have given phrase within their filepath
    * */
//    List<MediaQuery> findInFilePathQuery(String phrase);

    /*
    * Get MediaLink element by id
    * */
    MediaLink getLinkById(Long id);

    /*
    * Add new MediaLink element to database
    * */
    MediaLink addNewLink(MediaLink mediaLInk);

    MediaLink updateLink(MediaLink mediaLink);

    /*
    * Remove MediaLink element from database
    * */
    MediaLink removeLink(Long mediaLinkId);

    /*
    * Return all MediaLink elements from database
    * */
    List<MediaLink> getAllMediaLinks();

    /*
     * Return all MediaLink elements that have given phrase within their filepath
     * */
    public List<MediaLink> findInTargetPathLink(String phrase);

    public List<MediaLink> findInLinkPathLink(String phrase);

    /*
    * Return all MediaLink elements with given filepath
    * */
    MediaLink findMediaLinkByTargetPath(String filePath);

//    MediaLink addMediaIgnored(MediaLink mediaIgnored);
//
//    MediaLink removeMediaIgnored(Long id);
//
//    MediaLink getMediaIgnoredById(Long id);
//
//    List<MediaLink> getAllMediaIgnored();
//
//    MediaLink findMediaIgnoredByTargetPath(String filePath);


}

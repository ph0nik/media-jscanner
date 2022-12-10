package dao;

import model.MediaLink;

import java.util.List;

public interface MediaTrackerDao {

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

}

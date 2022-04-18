package service;

import model.*;
import util.MediaIdentity;
import util.MediaType;

import java.nio.file.Path;
import java.util.List;

public interface MediaLinksService extends Pagination {

    /*
     * Get list of all queries from db
     * */
    List<MediaQuery> getMediaQueryList();

    /*
     * For a given query perform online search for matching elements within given domain.
     * If first parameter is not empty it's going to be used as search phrase,
     * otherwise query from MediaQuery object is being used.
     * Returns List of QueryResult or null in case of exception.
     * */
    List<QueryResult> executeMediaQuery(String customQuery, long mediaQueryId, MediaIdentity mediaIdentity);

    /*
    * Returns results of latest request
    * */
    LastRequest getLatestMediaQuery();

    /*
     * Create symlink with specified query result and link properties
     * */
    MediaLink createSymLink(QueryResult queryResult, MediaIdentity mediaIdentity, MediaType mediaType);

    /*
    * Flag media query element as ignored.
    * This is intended for video files that user don't want to include in his collection,
    * for example trailers or video samples.
    * */
    MediaIgnored ignoreMediaFile(long mediaQueryId);

    /*
    * Returns list of all ignored media paths.
    * */
    List<MediaIgnored> getMediaIgnoredList();

    /*
    * Remove link and add target path back to the queue
    * */
    MediaQuery moveBackToQueue(long mediaLinkId);

    /*
    * Remove ignore flag and move back media file into the media queue.
    * */
    MediaQuery unIgnoreMedia(long mediaIgnoreId);

    /*
    * Returns list of existing media links
    * */
    List<MediaLink> getMediaLinks();

    /*
    * Scan current links folder and delete all links that
    * are not found in database.
    * Method removes symbolic links, all the non-media files within the same folder
    * and parent folder.
    * */
    void deleteInvalidLinks();

    boolean validatePath(Path path);

    void moveLinksToNewLocation(Path oldLinksFolder, Path newLinksFolder);


}

package service;

import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
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
    List<QueryResult> getLatestMediaQuery();

    /*
     * Create symlink with specified query result and link properties
     * */
    MediaLink createSymLink(QueryResult queryResult, MediaIdentity mediaIdentity, MediaType mediaType);

    /*
    * Remove link and add target path back to the queue
    * */
    MediaQuery moveBackToQueue(long mediaLinkId);

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

package service;

import model.*;
import util.MediaIdentity;

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
//    List<QueryResult> executeMediaQuery(String customQuery, MediaQuery mediaQuery, MediaIdentity mediaIdentity);

    /*
     * Executes media query search using web search engine and web api search engine.
     * Return results or empty list if nothing was found.
     * On connection error it returns query result elements with error description.
     * */
    List<QueryResult> executeMediaQuery(String customQuery, MediaIdentity mediaIdentity);

//    List<QueryResult> searchTmdbWithTitleAndYear(String customQuery, MediaQuery mediaQuery, MediaIdentity mediaIdentity, int year);

    List<QueryResult> searchTmdbWithTitleAndYear(String customQuery, MediaIdentity mediaIdentity, int year);

    /*
    * Returns results of latest request
    * */
    LastRequest getLatestMediaQueryRequest();

    /*
     * Create symlink with specified query result and link properties
     * */
//    LinkCreationResult createFileLink(QueryResult queryResult, MediaIdentity mediaIdentity, MediaType mediaType);

    List<LinkCreationResult> createFileLink(QueryResult queryResult, MediaIdentity mediaIdentity);

//    LinkCreationResult createFileLink(QueryResult queryResult, MediaIdentity mediaIdentifier, MediaType mediaType, int partNumber);

    LinkCreationResult createFileLink(QueryResult queryResult, MediaIdentity mediaIdentifier, MediaQuery mediaQuery);

    /*
    * Flag media query element as ignored.
    * This is intended for video files that user don't want to include in his collection,
    * for example trailers or video samples.
    * */
    MediaLink ignoreMediaFile(MediaQuery mediaQuery);

    /*
    * Returns list of all ignored media paths.
    * */
    List<MediaLink> getMediaIgnoredList();

    /*
    * Remove link and add target path back to the queue
    * */
    MediaQuery moveBackToQueue(long mediaLinkId);

    /*
    * Deletes original element of created link
    * */
    MediaLink deleteOriginalFile(long mediaLinkId);

    MediaLink restoreOriginalFile(long mediaLinkId);

    /*
    * Remove ignore flag and move back media file into the media queue.
    * */
    MediaQuery unIgnoreMedia(long mediaIgnoreId);

    /*
    * Returns list of existing media links
    * */
    List<MediaLink> getMediaLinks();

    List<MediaLink> searchMediaLinks(String search);

    /*
    * Checks if given path exists
    * */
    boolean validatePath(Path path);

    void moveLinksToNewLocation(Path oldLinksFolder, Path newLinksFolder);


    /*
    * Returns true if more than one media file belongs to the same directory at the same level
    * */
    boolean isMultipart(MediaQuery mediaQuery);
}

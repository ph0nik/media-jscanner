package service;

import model.LastRequest;
import model.MediaLink;
import model.OperationResult;
import model.QueryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import service.exceptions.NetworkException;
import service.query.MediaQueryService;
import service.query.TvQueryService;
import util.MediaIdentity;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

public interface MediaLinksService {

    /*
     * Executes media query search using web search engine and web api search engine.
     * Return results or empty list if nothing was found.
     * On connection error it returns query result elements with error description.
     * */
    List<QueryResult> executeMediaQuery(String customQuery, MediaIdentity mediaIdentity,
                                        MediaQueryService mediaQueryService) throws NetworkException;

    List<QueryResult> searchTmdbWithTitleAndYear(String customQuery,
                                                 MediaIdentity mediaIdentity,
                                                 int year,
                                                 MediaQueryService mediaQueryService) throws NetworkException;

    List<QueryResult> searchWithImdbId(String imdbId,
                                       MediaIdentity mediaIdentity,
                                       MediaQueryService mediaQueryService) throws NetworkException;


    // TODO create new service for getting info only, extend it with media query service
    QueryResult getTvDetails(QueryResult queryResult, int seasonNumber) throws NetworkException;

    /*
    * Based on query result and season number create media links for grouped media queries
    * */
    List<MediaLink> createMediaLinksTv(QueryResult queryResult, int seasonNumber,
                                       TvQueryService tvQueryService) throws FileNotFoundException;

    void setCurrentMediaLinks(List<MediaLink> mediaLinks);

    /*
    * Returns results of latest request
    * */
    LastRequest getLatestMediaQueryRequest();

    /*
     * Create symlink with specified query result and link properties
     * */
    int createFileLink(QueryResult queryResult,
                                         MediaIdentity mediaIdentity,
                                         MediaQueryService mediaQueryService) throws NetworkException;

    OperationResult createHardLinkWithDirectories(MediaLink mediaLink);

    /*
    * Flag media query element as ignored.
    * This is intended for video files that user don't want to include in his collection,
    * for example trailers or video samples.
    * */
    MediaLink ignoreMediaFile(MediaQueryService mediaQueryService);

    /*
    * Filter all ignored results with given query
    * */
    List<MediaLink> searchMediaIgnoredList(String query);

    /*
    * Returns list of all ignored media paths.
    * */
    List<MediaLink> getMediaIgnoredList();

    void clearInvalidIgnoreAndLinks();

    /*
    * Remove link and add target path back to the queue
    * */
    void moveBackToQueue(long mediaLinkId);

    /*
    * Deletes original element of created link
    * */
    MediaLink deleteOriginalFile(long mediaLinkId);

    MediaLink restoreOriginalFile(long mediaLinkId);

    /*
    * Remove ignore flag and move back media file into the media queue.
    * */
    void unIgnoreMedia(long mediaIgnoreId);

    /*
    * Returns list of existing media links
    * */
    List<MediaLink> getMediaLinks();

    Page<MediaLink> getPageableLinks(Pageable pageable, List<MediaLink> mediaQueryList);

    List<MediaLink> searchMediaLinks(String search);

    void removeEmptyFolders(String path);

    void moveLinksToNewLocation(Path oldLinksFolder, Path newLinksFolder);

}

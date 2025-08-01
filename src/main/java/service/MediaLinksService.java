package service;

import model.MediaLink;
import model.QueryResult;
import model.StatusDto;
import model.duplicates.DuplicateMediaLinkDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import service.exceptions.MissingReferenceMediaQueryException;
import service.exceptions.NetworkException;
import service.query.MediaQueryService;
import service.query.TvQueryService;
import util.MediaIdentifier;
import util.MediaType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface MediaLinksService {

    Page<MediaLink> getPageableLinksWithSorting(Pageable pageable, SortBy sortBy);

    public List<MediaLink> getMediaLinksToProcess();

    StatusDto getStatusDto();

    public void setMediaLinksToProcess(List<MediaLink> mediaLinksToProcess);

    void addMediaLinkToProcess(MediaLink mediaLink);

    public void clearMediaLinksToProcess();
    /*
     * Executes media query search using web search engine and web api search engine.
     * Return results or empty list if nothing was found.
     * On connection error it returns query result elements with error description.
     * */
    List<QueryResult> executeMediaQuery(String customQuery, MediaIdentifier mediaIdentifier,
                                        MediaQueryService mediaQueryService) throws NetworkException, MissingReferenceMediaQueryException;

    List<QueryResult> searchTmdbWithTitleAndYear(String customQuery,
                                                 MediaIdentifier mediaIdentifier,
                                                 int year,
                                                 MediaQueryService mediaQueryService) throws NetworkException, MissingReferenceMediaQueryException;

    List<QueryResult> searchWithImdbId(String imdbId,
                                       MediaIdentifier mediaIdentifier,
                                       MediaQueryService mediaQueryService) throws NetworkException, MissingReferenceMediaQueryException;


    List<QueryResult> multiSearchTmdb(
            String customQuery,
            MediaIdentifier mediaIdentifier,
            MediaQueryService mediaQueryService
    ) throws NetworkException, MissingReferenceMediaQueryException;

    QueryResult getTvDetails(QueryResult queryResult, int seasonNumber) throws NetworkException;

    /*
    * Based on query result and season number create media links for grouped media queries
    * */
    List<MediaLink> createMediaLinksTv(QueryResult queryResult, int seasonNumber,
                                       TvQueryService tvQueryService) throws FileNotFoundException;

    /*
    * Returns results of latest request
    * */
    List<QueryResult> getLatestMediaQueryRequest();

    void setLatestMediaQueryRequest(List<QueryResult> latestMediaQueryRequest);

    List<MediaLink> getErrorLinks();

    @SuppressWarnings("unchecked")
    List<MediaLink> getDuplicateLinks();

    // TODO get single dto, prompt user for change, check if there are more
    // and repeat until end of the list is reached
    DuplicateMediaLinkDto getNextDuplicateDto();

    void setDuplicateLinkToProcess(DuplicateMediaLinkDto duplicateDto);

    /*
     * Create symlink with specified query result and link properties
     * */
    List<MediaLink> createFileLinks(QueryResult queryResult,
                                    MediaIdentifier mediaIdentifier,
                                    MediaQueryService mediaQueryService) throws NetworkException;

    List<MediaLink> persistsCollectedMediaLinks(MediaQueryService mediaQueryService);

    boolean createHardLinkWithDirectories(MediaLink mediaLink) throws IOException;

    /*
    * Flag media query element as ignored.
    * This is intended for video files that user don't want to include in his collection,
    * for example trailers or video samples.
    * */
    int ignoreMediaFile(MediaQueryService mediaQueryService) throws MissingReferenceMediaQueryException;

    /*
    * Filter all ignored results with given query
    * */
    List<MediaLink> searchMediaIgnoredList(String query);

    /*
    * Returns list of all ignored media paths.
    * */
    List<MediaLink> getMediaIgnoredList();

    List<MediaLink> removeInvalidIgnoreAndLinks();

    List<MediaLink> getInvalidLinksForDeletion();

    List<MediaLink> getInvalidIgnoreForDeletion();

    void findInvalidLinks();

    void findInvalidIgnore();

    /*
    * Remove link and add target path back to the queue
    * */
    void moveBackToQueue(long mediaLinkId) throws IOException;

    /*
    * Deletes original element of created link
    * */
    MediaLink deleteOriginalFile(long mediaLinkId);

    MediaLink restoreOriginalFile(long mediaLinkId) throws IOException;

    /*
    * Remove ignore flag and move back media file into the media queue.
    * */
    void undoIgnoreMedia(long mediaIgnoreId);

    /*
    * Returns list of existing media links
    * */
    List<MediaLink> getMediaLinks();

    Page<MediaLink> getPageableLinks(Pageable pageable, List<MediaLink> mediaQueryList);

    List<MediaLink> searchMediaLinks(String search);

    @SuppressWarnings("unchecked")
    List<Path> getFoldersForClearing();

    void abortFolderClearing();

    boolean findEmptyFolders(MediaType mediaType) throws IOException;

    void persistRemoveEmptyFolders();

    void moveLinksToNewLocation(Path oldLinksFolder, Path newLinksFolder);

    void resetProcessLists();

    void clearInvalidLists();

    void findInvalidElements();
}

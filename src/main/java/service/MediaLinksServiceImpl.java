package service;

import dao.MediaTrackerDao;
import model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import service.exceptions.NetworkException;
import service.query.MediaQueryService;
import service.query.TvQueryService;
import util.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MediaLinksServiceImpl implements MediaLinksService {
    private static final Logger LOG = LoggerFactory.getLogger(MediaLinksServiceImpl.class);
    private static final int DEFAULT_YEAR_VALUE = 0; // 1000
    private final MediaTrackerDao mediaTrackerDao;
    private final CleanerService cleanerService;
    private final PropertiesService propertiesService;
    private final FileService fileService;
    private final ResponseParser responseParser;
    private final RequestService requestService;
    private final Pagination<MediaLink> pagination;
    private final MediaIdentity linkIdentifier = MediaIdentity.IMDB;
    // TODO separate all data from service,
    private LastRequest lastRequest;
    private QueryResult currentQueryResult;
    private List<MediaLink> currentMediaLinks = new LinkedList<>();

    public MediaLinksServiceImpl(@Qualifier("spring") MediaTrackerDao dao,
                                 PropertiesService propertiesService,
                                 CleanerService cleanerService,
                                 FileService fs,
                                 Pagination<MediaLink> pagination,
                                 RequestService requestService,
                                 ResponseParser responseParser) {
        this.mediaTrackerDao = dao;
        this.propertiesService = propertiesService;
        this.cleanerService = cleanerService;
        this.fileService = fs;
        this.pagination = pagination;
        this.requestService = requestService;
        this.responseParser = responseParser;
    }

    @Override
    public Page<MediaLink> getPageableLinks(Pageable pageable, List<MediaLink> mediaLinkList) {
        return pagination.getPage(pageable, mediaLinkList);
    }

    public QueryResult getCurrentQueryResult() {
        return currentQueryResult;
    }

    public void setCurrentQueryResult(QueryResult currentQueryResult) {
        this.currentQueryResult = currentQueryResult;
    }

    /*
     * Executes media query search using web search engine and web api search engine.
     * Return results or empty list if nothing was found.
     * On connection error it returns query result elements with error description.
     * */
    @Override
    public List<QueryResult> executeMediaQuery(String customQuery, MediaIdentity mediaIdentity,
                                               MediaQueryService mediaQueryService)
            throws NetworkException {
        MediaQuery mediaQuery = mediaQueryService.getReferenceQuery();
        List<QueryResult> combinedSearchResults = List.of();
        if (mediaQuery.getMediaType() == MediaType.MOVIE) {
            List<QueryResult> webSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.WEB_SEARCH);
            combinedSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.TMDB_API);
            combinedSearchResults.addAll(webSearchResults);
        }
        if (mediaQuery.getMediaType() == MediaType.TV) {
            List<QueryResult> webSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.WEB_SEARCH);
            combinedSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.TMDB_API_TV);
            combinedSearchResults.addAll(webSearchResults);
        }
        lastRequest = new LastRequest(combinedSearchResults, mediaQuery);
        return combinedSearchResults;
    }

    @Override
    public List<QueryResult> searchTmdbWithTitleAndYear(String customQuery,
                                                        MediaIdentity mediaIdentity,
                                                        int year,
                                                        MediaQueryService mediaQueryService)
            throws NetworkException {
        LOG.info("[ search_with_year ] {} | {} | {}", customQuery, mediaIdentity, year);
        MediaQuery referenceQuery = mediaQueryService.getReferenceQuery();
        if (referenceQuery.getMediaType() == MediaType.MOVIE) {
            return generalSearchRequest(customQuery, referenceQuery,
                    mediaIdentity, year, SearchType.TMDB_API);
        }
        return generalSearchRequest(customQuery, referenceQuery,
                mediaIdentity, year, SearchType.TMDB_API_TV_YEAR);
    }

    @Override
    public List<QueryResult> searchWithImdbId(String imdbLink,
                                              MediaIdentity mediaIdentity,
                                              MediaQueryService mediaQueryService)
            throws NetworkException {
        String imdbId = TextExtractTools.getImdbIdFromLink(imdbLink);
        LOG.info("[ search_with_imdb_id ] {}", imdbId);
        return generalSearchRequest(imdbId, mediaQueryService.getReferenceQuery(),
                mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.TMDB_API_IMDB_ID);
    }

    /*
     * General request method. Performs search based on search type enum.
     * If any of requests throws exception, an error information is returned as QueryResult object to client.
     * */
    List<QueryResult> generalSearchRequest(String customQuery, MediaQuery mediaQuery,
                                           MediaIdentity mediaIdentity, int year, SearchType searchType)
            throws NetworkException {
        String query = (customQuery.isEmpty()) ? mediaQuery.getQuery() : customQuery;
        String filePath = mediaQuery.getFilePath();
        try {
            String searchResultsJson;
            if (searchType == SearchType.TMDB_API) { // title and year search with tmdb
                searchResultsJson = requestService.tmdbApiTitleAndYearMovie(query, year);
                return responseParser.parseTmdbApiMovieResults(searchResultsJson, filePath);
            }
            if (searchType == SearchType.TMDB_API_IMDB_ID) { // imdb id search with tmdb
                QueryResult queryResult = new QueryResult(filePath);
                queryResult.setImdbId(customQuery);
                searchResultsJson = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentity);
                return responseParser.parseTmdbApiWithImdbId(searchResultsJson, queryResult);
            }
            if (searchType == SearchType.TMDB_API_TV) { // initial search for tv
                searchResultsJson = requestService.tmdbApiTvRequest(
                        TextExtractTools.extractTitleFromTvElement(query));
                return responseParser.parseTmdbApiTvResults(searchResultsJson, filePath);
            }
            if (searchType == SearchType.TMDB_API_TV_YEAR) { // search with title and year for tv
                searchResultsJson = requestService.tmdbApiTitleAndYearTv(query, year);
                return responseParser.parseTmdbApiTvResults(searchResultsJson, filePath);
            }
            searchResultsJson = requestService.webSearchRequest(query); // web search with phrase
            if (mediaQuery.getMediaType() == MediaType.TV) {
                return responseParser.parseTvWebResults(searchResultsJson, filePath);
            }
            return responseParser.parseMovieWebResults(searchResultsJson, filePath);
//        }
//        catch (HttpStatusException e) {
//            LOG.error("[ general_search ] Connection error: {} @ {}", e.getStatusCode(), e.getUrl());
//        }
//        catch (UnknownHostException e) {
//            String errorMessage = "[ general_search ] Host not found: " + e.getMessage();
//            LOG.error(errorMessage);
        } catch (IOException e) {
            String errorMessage = "[ general_search ] IO Exception: " + e.getMessage();
            LOG.error(errorMessage);
        }
        return new LinkedList<>();
    }

    // TODO create new service for getting info only, extend it with media query service
    @Override
    public QueryResult getTvDetails(QueryResult queryResult, int seasonNumber) throws NetworkException {
//        int seasonNumber = TextExtractTools.extractSeasonNumber(queryResult.getOriginalPath());
//        LOG.info("[ episode_list ] Seasons extracted: {}", seasonNumber);

        if (queryResult.getTheMovieDbId() == 0) { // if no tmdb id found send request with imdb id
            LOG.info("[ episode_list ] No tmdb id found, sending request with imdb id: {}", queryResult.getImdbId());
            String responseJson = requestService.tmdbApiRequestWithSpecifiedId(queryResult, MediaIdentity.IMDB);
            List<QueryResult> queryResults = responseParser.parseTmdbApiWithImdbId(responseJson, queryResult);
            queryResult = queryResults.stream().findFirst().orElse(null);
        }
        if (queryResult != null) {
            LOG.info("[ episode_list ] Found matching episodes.");
            String response = requestService.tmdbGetSeasonDetails(queryResult);
            queryResult = responseParser.parseTvDetail(queryResult, response, seasonNumber);
            // if episodecount == 0 thore ex for incorrect season
        }
        setCurrentQueryResult(queryResult);
        return queryResult;
    }

    /*
     * Based on query result and season number create media links for grouped media queries
     * */
    @Override
    public List<MediaLink> createMediaLinksTv(QueryResult queryResult, int seasonNumber,
                                              TvQueryService tvQueryService) throws FileNotFoundException {
        List<MediaQuery> groupedQueries = tvQueryService.getGroupedQueriesWithChild(queryResult.getOriginalPath());
        List<MediaLink> mediaLinks = new LinkedList<>();
        fileService.setLinksRootFolder(propertiesService.getLinksFolderTv());
        int index = 1;
        for (MediaQuery mq : groupedQueries) {
            queryResult.setOriginalPath(mq.getFilePath());
            Path tvEpisodePath = fileService.createTvEpisodePath(queryResult, seasonNumber, index++, MediaIdentity.IMDB);
            MediaLink mediaLink = new MediaLink();
            mediaLink.setOriginalPath(mq.getFilePath());
            mediaLink.setLinkPath(tvEpisodePath.toString());
            mediaLink.setTheMovieDbId(queryResult.getTheMovieDbId());
            mediaLink.setImdbId(queryResult.getImdbId());
            mediaLinks.add(mediaLink);
        }
        return mediaLinks;
    }

    @Override
    public void setCurrentMediaLinks(List<MediaLink> mediaLinks) {
        currentMediaLinks = mediaLinks;
    }

//    public List<MediaLink> editExistingMediaLinksTv(MediaLinkDto mediaLinkDto) {
//        List<TvMediaLinkDto> mediaLinkDtos = mediaLinkDto.getMediaLinkDtos();
//        for (TvMediaLinkDto tvMediaLinkDto : mediaLinkDtos) {
//            MediaLink mediaLink = tvMediaLinkDto.getMediaLink();
//
//        }
//    }

    @Override
    public LastRequest getLatestMediaQueryRequest() {
        return lastRequest;
    }

    @Override
    public int createFileLink(QueryResult queryResult,
                              MediaIdentity mediaIdentity,
                              MediaQueryService mediaQueryService)
            throws NetworkException {
        int processed = 0;
        for (MediaQuery mq : mediaQueryService.getProcessList()) {
            createFileLink(queryResult, mediaIdentity, mq, mediaQueryService);
            processed++;
        }
        return processed;
        // TODO custom model with ok/error response
    }

    // TODO separate elements of the process, get the data and return list of medialinks with given
    // results, then process list and create actual links
    OperationResult createFileLink(QueryResult queryResult,
                                   MediaIdentity mediaIdentifier,
                                   MediaQuery mediaQuery,
                                   MediaQueryService mediaQueryService)
            throws NetworkException {
        // naming pattern -> Film (2018) [tmdbid-65567]
        // send request to themoviedb api with given query result
        queryResult.setMultipart(mediaQuery.getMultipart());
        queryResult.setMediaType(mediaQuery.getMediaType());
        queryResult.setOriginalPath(mediaQuery.getFilePath());
        OperationResult operationResult;
        queryResult = getSelectionDetails(queryResult, mediaIdentifier);
        try {
            MediaLink mediaLink = createFilePaths(queryResult);
            operationResult = createHardLinkWithDirectories(mediaLink);
            if (operationResult.isCreationStatus() && !linkRecordExist(mediaLink)) {
                mediaTrackerDao.addNewLink(mediaLink);
                mediaQueryService.removeQueryByFilePath(queryResult.getOriginalPath());
            }
        } catch (FileNotFoundException e) {
            String errorMessage = "[ file_service ] No links root folder defined.";
            LOG.error(errorMessage);
            operationResult = new OperationResult(false, errorMessage, new MediaLink());
        }
        return operationResult;
    }

    boolean linkRecordExist(MediaLink mediaLink) {
        return getMediaLinks().stream()
                .anyMatch(ml ->
                        ml.getLinkPath().equals(mediaLink.getLinkPath()) &&
                                ml.getOriginalPath().equals(mediaLink.getOriginalPath()));
    }

    MediaLink createFilePaths(QueryResult queryResult) throws FileNotFoundException {
        Path originalPath = Path.of(queryResult.getOriginalPath());
        Path linkPath = Path.of("");
        fileService.setLinksRootFolder(propertiesService.getLinksFolderMovie());
        if (queryResult.getMediaType() == MediaType.MOVIE) {
            linkPath = fileService.createMovieLinkPath(queryResult, linkIdentifier);
        }
        if (queryResult.getMediaType() == MediaType.EXTRAS) {
            linkPath = fileService.createExtrasLinkPath(queryResult, linkIdentifier);
        }
        MediaLink mediaLink = new MediaLink();
        mediaLink.setOriginalPath(originalPath.toString());
        mediaLink.setLinkPath(linkPath.toString());
        mediaLink.setTheMovieDbId(queryResult.getTheMovieDbId());
        mediaLink.setImdbId(queryResult.getImdbId());
        return mediaLink;
    }

    QueryResult getSelectionDetails(QueryResult queryResult, MediaIdentity mediaIdentifier)
            throws NetworkException {
        String response = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentifier);
        LOG.info("[ link ] extracting json data...");
        LOG.info("[ link ] {}", queryResult);
        if (mediaIdentifier == MediaIdentity.TMDB) {
            queryResult = responseParser.parseDetailsRequestByTmdbId(queryResult, response);
        }
        if (mediaIdentifier == MediaIdentity.IMDB) {
            queryResult = responseParser.parseDetailsRequestByExternalId(queryResult, response);
        }
        return queryResult;
    }

    @Override
    public OperationResult createHardLinkWithDirectories(MediaLink mediaLink) {
        OperationResult lcr = createHardLinkWithDirectories(
                Path.of(mediaLink.getLinkPath()), Path.of(mediaLink.getOriginalPath()));
        lcr.setMediaLink(mediaLink);
        return lcr;
//        return createHardLinkWithDirectories(mediaLink, false);
    }

    /*
     * Creates hard link with parameters provided in MediaLink object.
     * Returns LinkCreationResult which contains result status (true for success and false for failure),
     * optional error message and original MediaLink object.
     * Params:   mediaLink - object containing prerequisites for creating link
     *           existingLink - boolean value representing current state of link, for existing links
     *           use true to invert and recreate original, source file.
     * */
    public OperationResult createHardLinkWithDirectories(Path linkPath, Path incomingPath) {
        Path parentLinkPath = linkPath.getParent();
        if (!Files.exists(incomingPath)) {
            LOG.error("[ link ] No original file found with path: {}", incomingPath);
            return new OperationResult(false, "no original");
        }
        try {
            if (!Files.exists(parentLinkPath)) {
                Files.createDirectories(parentLinkPath);
                LOG.info("[ link ] creating folder...: {}", parentLinkPath);
            }
            LOG.info("[ link ] creating link...");
            Files.createLink(linkPath, incomingPath);
            LOG.info("[ link ] link created: {} => {}", linkPath, incomingPath);
            return new OperationResult(true, "New link added");
        } catch (FileAlreadyExistsException e) {
            LOG.error("[ link ] Link already exists: {}", e.getMessage());
            return new OperationResult(false, "File already exists: " + e.getMessage());
        } catch (IOException | SecurityException e) {
            LOG.error(e.getMessage());
            return new OperationResult(false, e.getMessage());
        }
    }

    @Override
    public MediaLink ignoreMediaFile(MediaQueryService mediaQueryService) {
        MediaQuery mediaQuery = mediaQueryService.getReferenceQuery();
        MediaLink mediaIgnored = new MediaLink();
        mediaIgnored.setOriginalPath(mediaQuery.getFilePath());
        mediaIgnored.setLinkPath("ignore");
        mediaIgnored.setImdbId("none");
        mediaIgnored.setTheMovieDbId(-1);
        mediaIgnored.setOriginalPresent(true);
        mediaTrackerDao.addNewLink(mediaIgnored);
        LOG.info("[ ignore ] Ignored element: {}", mediaQuery.getFilePath());
        mediaQueryService.removeQueryFromQueue(mediaQuery);
        return mediaTrackerDao.getMediaLinkByTargetPath(mediaQuery.getFilePath());
    }

    @Override
    public List<MediaLink> getMediaLinks() {
        return filterMediaLinks(false).collect(Collectors.toList());
    }

    @Override
    public List<MediaLink> getMediaIgnoredList() {
        return filterMediaLinks(true).collect(Collectors.toList());
    }

    Stream<MediaLink> filterMediaLinks(boolean ignoredOnly) {
        return (ignoredOnly)
                ? getAllMediaLinks(ml -> ml.getLinkPath().equals("ignore"))
                : getAllMediaLinks(ml -> !ml.getLinkPath().equals("ignore"));
    }

    /*
     * Returns stream of media links based on given predicate, at each call validates
     * original files presence.
     * */
    Stream<MediaLink> getAllMediaLinks(Predicate<MediaLink> mediaLinkSwitch) {
        return mediaTrackerDao.getAllMediaLinks()
                .stream()
                .filter(mediaLinkSwitch)
                .map(this::validateLink);
    }

    List<MediaLink> searchMediaLinks(String phrase, boolean ignoredOnly) {
        String[] s = phrase.split(" ");
        return filterMediaLinks(ignoredOnly)
                .filter(ml ->
                        ml.getOriginalPath().toLowerCase().contains(phrase.toLowerCase()) ||
                                ml.getLinkPath().toLowerCase().contains(phrase.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MediaLink> searchMediaIgnoredList(String query) {
        return searchMediaLinks(query, true);
    }

    @Override
    public List<MediaLink> searchMediaLinks(String query) {
        return searchMediaLinks(query, false);
    }

    @Override
    public void clearInvalidIgnoreAndLinks() {
        clearInvalidIgnore();
        clearInvalidLinks();
    }

    /*
     * Remove ignore media record if original file is no longer present.
     * */
    void clearInvalidIgnore() {
        getMediaIgnoredList().stream()
                .filter(mi -> !mi.isOriginalPresent())
                .forEach(mi -> mediaTrackerDao.removeLink(mi.getMediaId()));
    }

    /*
     * Remove link record if neither original nor link file exists.
     * */
    void clearInvalidLinks() {
        // TODO add log info about deleted links
        getMediaLinks().stream()
                .filter(ml -> !ml.isOriginalPresent())
                .filter(ml -> !fileService.validatePath(ml.getLinkPath()))
                .forEach(ml -> mediaTrackerDao.removeLink(ml.getMediaId()));
    }

    // TODO rescan existing links to update link status
    // TODO when scanning for new files show creation date (last modified) and file size
    @Override
    public void moveBackToQueue(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        OperationResult operationResult;
        if (!mediaLink.isOriginalPresent()) { // if original is not present recreate it
            LOG.info("[ remove_link ] File not found, recreating original file...");
            operationResult = createHardLinkWithDirectories(Path.of(mediaLink.getOriginalPath()), Path.of(mediaLink.getLinkPath()));
            operationResult.setMediaLink(mediaLink);
        }
        mediaTrackerDao.removeLink(mediaLinkId); // remove from db
        Path elementToBeRemoved = Path.of(mediaLink.getLinkPath());
        cleanerService.deleteSingleFile(elementToBeRemoved);
        cleanerService.clearEmptyFolders(elementToBeRemoved.getParent()); // clear parent folder
        LOG.info("[ remove_link ] Link removed for file: {}", mediaLink.getOriginalPath());
    }

    @Override
    public MediaLink deleteOriginalFile(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        cleanerService.deleteSingleFile(Path.of(mediaLink.getOriginalPath()));
        cleanerService.clearEmptyFolders(Path.of(mediaLink.getOriginalPath()).getParent());
        LOG.info("[ delete_original ] Original file deleted: {}", mediaLink.getOriginalPath());
        mediaLink.setOriginalPresent(false);
        mediaTrackerDao.updateLink(mediaLink);
        return mediaLink;
    }

    @Override
    public MediaLink restoreOriginalFile(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        createHardLinkWithDirectories(Path.of(mediaLink.getOriginalPath()), Path.of(mediaLink.getLinkPath()));
        LOG.info("[ restore_original ] Original file restored: {}", mediaLink.getOriginalPath());
        mediaLink.setOriginalPresent(true);
        mediaTrackerDao.updateLink(mediaLink);
        return mediaLink;
    }

    @Override
    public void unIgnoreMedia(long mediaIgnoreId) {
        MediaLink mediaLink = mediaTrackerDao.removeLink(mediaIgnoreId);
        LOG.info("[ remove_link ] Link removed for file: {}", mediaLink.getOriginalPath());

    }

    /*
     * Checks if original path of given media link exists and updates boolean value if needed.
     * */
    MediaLink validateLink(MediaLink mediaLink) {
        if (!fileService.validatePath(mediaLink.getOriginalPath()) && mediaLink.isOriginalPresent()) {
            mediaLink.setOriginalPresent(false);
            mediaTrackerDao.updateLink(mediaLink);
        }
        return mediaLink;
    }

    @Override
    public void removeEmptyFolders(String path) {
        cleanerService.clearEmptyFolders(Path.of(path));
    }

    /*
     *
     * */
    public void moveLinksToNewLocation(Path oldLinksFolder, Path newLinksFolder) {
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        for (MediaLink ml : allMediaLinks) {
            Path oldLinkPath = Path.of(ml.getLinkPath());
            String newLinkString = oldLinkPath.toString().replace(oldLinksFolder.toString(), newLinksFolder.toString());
            Path newLinkPath = Path.of(newLinkString);
            try {
                if (!Files.exists(newLinkPath.getParent())) {
                    Files.createDirectories(newLinkPath.getParent());
                    LOG.info("[ link ] Creating folder...: {}", newLinkPath.getParent());
                }
                Files.createSymbolicLink(newLinkPath, Path.of(ml.getOriginalPath()));
                LOG.info("[ link ] Creating link... {}", newLinkPath);
                ml.setLinkPath(newLinkString);
                MediaLink mediaLink = mediaTrackerDao.updateLink(ml);
                cleanerService.deleteSingleFile(oldLinkPath);
                cleanerService.clearEmptyFolders(oldLinkPath.getParent());
                LOG.info("[ link ] Link moved to a new folder: {}", mediaLink);
            } catch (FileAlreadyExistsException e) {
                LOG.error("[ link ] Link already exists: {}", e.getMessage());
                new OperationResult(false, e.getMessage(), ml);
            } catch (IOException | SecurityException e) {
                LOG.error(e.getMessage());
                new OperationResult(false, e.getMessage(), ml);
            }
        }
    }
}

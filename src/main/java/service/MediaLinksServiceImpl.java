package service;

import dao.MediaTrackerDao;
import model.*;
import model.validator.RequiredFieldException;
import model.validator.Validator;
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
import java.util.ArrayList;
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
    // TODO separate all data from service, create LiveData class
    // store all the data that is not persisted to db
    private LastRequest lastRequest;
    private QueryResult currentQueryResult;

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
                    mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.WEB_SEARCH_MOVIE);
            combinedSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.TMDB_API);
            combinedSearchResults.addAll(webSearchResults);
        }
        if (mediaQuery.getMediaType() == MediaType.TV) {
            List<QueryResult> webSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.WEB_SEARCH_TV);
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
            switch (searchType) {
                case TMDB_API:
                    searchResultsJson = requestService.tmdbApiTitleAndYearMovie(query, year);
                    return responseParser.parseTmdbApiMovieResults(searchResultsJson, filePath);
                case TMDB_API_IMDB_ID:
                    QueryResult queryResult = new QueryResult(filePath);
                    queryResult.setImdbId(customQuery);
                    searchResultsJson = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentity);
                    return responseParser.parseTmdbApiWithImdbId(searchResultsJson, queryResult);
                case TMDB_API_TV:
                    searchResultsJson = requestService.tmdbApiTvRequest(
                            TextExtractTools.extractTitleFromTvElement(query));
                    return responseParser.parseTmdbApiTvResults(searchResultsJson, filePath);
                case TMDB_API_TV_YEAR:
                    searchResultsJson = requestService.tmdbApiTitleAndYearTv(query, year);
                    return responseParser.parseTmdbApiTvResults(searchResultsJson, filePath);
                case WEB_SEARCH_MOVIE:
                    searchResultsJson = requestService.webSearchRequest(query); // web search with phrase
                    return responseParser.parseMovieWebResults(searchResultsJson, filePath);
                case WEB_SEARCH_TV:
                    searchResultsJson = requestService.webSearchRequest(query); // web search with phrase
                    return responseParser.parseTvWebResults(searchResultsJson, filePath);
            }
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
        int index = 1;
        for (MediaQuery mq : groupedQueries) {
            queryResult.setOriginalPath(mq.getFilePath());
            Path tvEpisodePath = fileService.createTvEpisodePath(
                    queryResult,
                    seasonNumber,
                    index++,
                    MediaIdentity.IMDB,
                    propertiesService.getLinksFolderTv()
            );
            MediaLink mediaLink = new MediaLink();
            mediaLink.setOriginalPath(mq.getFilePath());
            mediaLink.setLinkPath(tvEpisodePath.toString());
            mediaLink.setTheMovieDbId(queryResult.getTheMovieDbId());
            mediaLink.setImdbId(queryResult.getImdbId());
            mediaLinks.add(mediaLink);
        }
        return mediaLinks;
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
    public List<MediaLink> createFileLink(QueryResult queryResult,
                                          MediaIdentity mediaIdentity,
                                          MediaQueryService mediaQueryService)
            throws NetworkException {
        List<MediaLink> mediaLinksToProcess = new ArrayList<>();
        for (MediaQuery mq : mediaQueryService.getProcessList()) {
            MediaLink mediaLink = createFileLink(queryResult, mediaIdentity, mq, mediaQueryService);
            if (mediaLink != null) mediaLinksToProcess.add(mediaLink);
            // if some data needed to create filename is missing, skip it
        }
//        mediaQueryService.setMediaLinksToProcess(mediaLinksToProcess);
        return mediaLinksToProcess;
    }

    MediaLink createFileLink(QueryResult queryResult,
                                   MediaIdentity mediaIdentifier,
                                   MediaQuery mediaQuery,
                                   MediaQueryService mediaQueryService)
            throws NetworkException {
        queryResult.setMultipart(mediaQuery.getMultipart());
        queryResult.setMediaType(mediaQuery.getMediaType());
        queryResult.setOriginalPath(mediaQuery.getFilePath());
        queryResult = getSelectionDetails(queryResult, mediaIdentifier);
        try {
            return createLinkPath(queryResult);
        } catch (RequiredFieldException | IllegalAccessException e) { // temporary
            LOG.error("[ media_links_service ] Empty media link field: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void persistsCollectedMediaLinks(MediaQueryService mediaQueryService) {
        for (MediaLink ml : mediaQueryService.getMediaLinksToProcess()) {
            try {
                Validator.validateForNulls(ml);
                if (createHardLinkWithDirectories(ml)) {
                    mediaTrackerDao.addNewLink(ml);
                    mediaQueryService.removeQueryByFilePath(ml.getOriginalPath());
                }
            } catch (RequiredFieldException | IllegalAccessException e) {
                LOG.error("[ media_links_service ] Empty media link field: {}", e.getMessage());
            } catch (IOException e) {
                LOG.error("[ media_links_service ] Error creating link: {}", e.getMessage());
            }
        }
    }

    boolean linkRecordExist(MediaLink mediaLink) {
        return getMediaLinks().stream()
                .anyMatch(ml ->
                        ml.getLinkPath().equals(mediaLink.getLinkPath()) &&
                                ml.getOriginalPath().equals(mediaLink.getOriginalPath()));
    }

    MediaLink createLinkPath(QueryResult queryResult)
            throws RequiredFieldException, IllegalAccessException {
        Path originalPath = Path.of(queryResult.getOriginalPath());
        Path linkPath = Path.of("");
//        fileService.setLinksRootFolder(propertiesService.getLinksFolderMovie());
        if (queryResult.getMediaType() == MediaType.MOVIE) {
            linkPath = fileService.createMovieLinkPath_new(
                    queryResult,
                    linkIdentifier,
                    propertiesService.getLinksFolderMovie()
            );
        }
        if (queryResult.getMediaType() == MediaType.EXTRAS) {
            linkPath = fileService.createMovieLinkPath_new(
                    queryResult,
                    linkIdentifier,
                    propertiesService.getLinksFolderMovie()
            );
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
    public boolean createHardLinkWithDirectories(MediaLink mediaLink) throws IOException {
        return createHardLinkWithDirectories(
                Path.of(mediaLink.getLinkPath()),
                Path.of(mediaLink.getOriginalPath())
        );
    }

    /*
     * Creates hard link with parameters provided in MediaLink object.
     * Returns LinkCreationResult which contains result status (true for success and false for failure),
     * optional error message and original MediaLink object.
     * Params:   mediaLink - object containing prerequisites for creating link
     *           existingLink - boolean value representing current state of link, for existing links
     *           use true to invert and recreate original, source file.
     * */
    public boolean createHardLinkWithDirectories(Path linkPath, Path incomingPath)
            throws IOException, SecurityException  {
        Path parentLinkPath = linkPath.getParent();
        if (!Files.exists(incomingPath)) {
            LOG.error("[ link ] No original file found with path: {}", incomingPath);
            return false;
        }
            if (!Files.exists(parentLinkPath)) {
                Files.createDirectories(parentLinkPath);
                LOG.info("[ link ] creating folder...: {}", parentLinkPath);
            }
            LOG.info("[ link ] creating link...");
            Files.createLink(linkPath, incomingPath);
            LOG.info("[ link ] link created: {} => {}", linkPath, incomingPath);
            return true;
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
                .filter(ml -> !fileService.doesPathExist(ml.getLinkPath()))
                .forEach(ml -> mediaTrackerDao.removeLink(ml.getMediaId()));
    }

    // TODO rescan existing links to update link status
    // TODO when scanning for new files show creation date (last modified) and file size
    @Override
    public void moveBackToQueue(long mediaLinkId) throws IOException {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        if (!mediaLink.isOriginalPresent()) { // if original is not present recreate it
            LOG.info("[ remove_link ] File not found, recreating original file...");
            createHardLinkWithDirectories(Path.of(mediaLink.getOriginalPath()), Path.of(mediaLink.getLinkPath()));
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
    public MediaLink restoreOriginalFile(long mediaLinkId) throws IOException {
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
        if (!fileService.doesPathExist(mediaLink.getOriginalPath()) && mediaLink.isOriginalPresent()) {
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
     * TODO
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

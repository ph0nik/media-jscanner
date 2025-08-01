package service;

import dao.MediaTrackerDao;
import model.*;
import model.duplicates.DuplicateMediaLinkDto;
import model.validator.RequiredFieldException;
import model.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import service.exceptions.MissingReferenceMediaQueryException;
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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MediaLinksServiceImpl extends CounterCacheService implements MediaLinksService {
    private static final Logger LOG = LoggerFactory.getLogger(MediaLinksServiceImpl.class);
    private static final int DEFAULT_YEAR_VALUE = 0; // 1000
    private static final String LAST_REQUEST_KEY = "last-request";
    private static final String LINKS_TO_PROCESS_KEY = "links-to-process";
    private static final String DUPLICATE_LINKS_KEY = "duplicate-links";
    private static final String CHANGE_LINKS_KEY = "change-links";
    private static final String CHANGE_IGNORE_KEY = "change-ignored";
    private static final String ERROR_LINKS_KEY = "error-links";
    private static final String INVALID_LINKS_KEY = "invalid-links";
    private static final String INVALID_IGNORE_KEY = "invalid-ignore";
    private static final String EMPTY_FOLDERS = "empty-folders";
    private final MediaTrackerDao mediaTrackerDao;
    private final CleanerService cleanerService;
    private final PropertiesService propertiesService;
    private final FileService fileService;
    private final ResponseParser responseParser;
    private final RequestService requestService;
    private final DuplicateResolverServiceImpl duplicateResolverService;
    private final Pagination<MediaLink> pagination;
    private final MediaIdentifier linkIdentifier = MediaIdentifier.IMDB;

    public MediaLinksServiceImpl(@Qualifier("jpa") MediaTrackerDao dao,
                                 PropertiesService propertiesService,
                                 CleanerService cleanerService,
                                 FileService fs,
                                 Pagination<MediaLink> pagination,
                                 RequestService requestService,
                                 ResponseParser responseParser,
                                 DuplicateResolverServiceImpl duplicateResolverService,
                                 CacheManager cacheManager) {
        super(cacheManager);
        this.mediaTrackerDao = dao;
        this.propertiesService = propertiesService;
        this.cleanerService = cleanerService;
        this.fileService = fs;
        this.pagination = pagination;
        this.requestService = requestService;
        this.responseParser = responseParser;
        this.duplicateResolverService = duplicateResolverService;
        //    private QueryResult currentQueryResult;
    }

    @Override
    public Page<MediaLink> getPageableLinks(Pageable pageable, List<MediaLink> mediaLinkList) {
        return pagination.getPage(pageable, mediaLinkList);
    }

    private Comparator<MediaLink> getComparator(SortBy sortBy) {
        Comparator<MediaLink> comparator;
        if (sortBy == SortBy.LINK_PATH) {
            comparator = Comparator.comparing(MediaLink::getLinkPath);
        } else if (sortBy == SortBy.SOURCE_PATH) {
            comparator = Comparator.comparing(MediaLink::getOriginalPath);
        } else {
            comparator = Comparator.comparing(MediaLink::isOriginalPresent)
                    .reversed()
                    .thenComparing(MediaLink::getLinkPath);
        }
        return comparator;
    }

    @Override
    public Page<MediaLink> getPageableLinksWithSorting(Pageable pageable, SortBy sortBy) {
        getComparator(sortBy);
        List<MediaLink> allMediaLinks = getMediaLinks()
                .stream()
                .sorted(getComparator(sortBy))
                .collect(Collectors.toList());
        return pagination.getPage(pageable, allMediaLinks);
    }


    @SuppressWarnings("unchecked")
    @Override
    public List<MediaLink> getMediaLinksToProcess() {
        List<MediaLink> fromCache = getFromCache(
                LINKS_TO_PROCESS_KEY,
                List.class);
        return (fromCache == null) ? List.of() : fromCache;
    }

    @Override
    public StatusDto getStatusDto() {
        return new StatusDto(
                getMediaLinks().size(),
                getMediaIgnoredList().size(),
                getChangedLinksCount(),
                getChangedIgnoreCount()
        );
    }


    @Override
    public void setMediaLinksToProcess(List<MediaLink> mediaLinksToProcess) {
        updateCache(LINKS_TO_PROCESS_KEY, mediaLinksToProcess);
    }

    @Override
    public void addMediaLinkToProcess(MediaLink mediaLink) {
        if (getMediaLinksToProcess() == null) {
            setMediaLinksToProcess(List.of(mediaLink));
        } else {
            List<MediaLink> mediaLinksToProcess = new ArrayList<>(getMediaLinksToProcess());
            mediaLinksToProcess.add(mediaLink);
            setMediaLinksToProcess(mediaLinksToProcess);
        }
    }

    @Override
    public void clearMediaLinksToProcess() {
        clearCache(LINKS_TO_PROCESS_KEY);
    }

    /*
     * Executes media query search using web search engine and web api search engine.
     * Return results or empty list if nothing was found.
     * On connection error it returns query result elements with error description.
     * */
    @Override
    public List<QueryResult> executeMediaQuery(String customQuery, MediaIdentifier mediaIdentifier,
                                               MediaQueryService mediaQueryService)
            throws NetworkException, MissingReferenceMediaQueryException {
        MediaQuery mediaQuery = mediaQueryService.getReferenceQuery();
        List<QueryResult> combinedSearchResults = List.of();
        if (mediaQuery.getMediaType() == MediaType.MOVIE) {
            List<QueryResult> webSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentifier, DEFAULT_YEAR_VALUE, SearchType.WEB_SEARCH_MOVIE);
            combinedSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentifier, DEFAULT_YEAR_VALUE, SearchType.TMDB_API);
            combinedSearchResults.addAll(webSearchResults);
        }
        if (mediaQuery.getMediaType() == MediaType.TV) {
            List<QueryResult> webSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentifier, DEFAULT_YEAR_VALUE, SearchType.WEB_SEARCH_TV);
            combinedSearchResults = generalSearchRequest(customQuery, mediaQuery,
                    mediaIdentifier, DEFAULT_YEAR_VALUE, SearchType.TMDB_API_TV);
            combinedSearchResults.addAll(webSearchResults);
        }
        setLatestMediaQueryRequest(combinedSearchResults);
        return combinedSearchResults;
    }

    @Override
    public List<QueryResult> searchTmdbWithTitleAndYear(String customQuery,
                                                        MediaIdentifier mediaIdentifier,
                                                        int year,
                                                        MediaQueryService mediaQueryService)
            throws NetworkException, MissingReferenceMediaQueryException {
        LOG.info("[ search_with_year ] {} | {} | {}", customQuery, mediaIdentifier, year);
        MediaQuery referenceQuery = mediaQueryService.getReferenceQuery();
        if (referenceQuery.getMediaType() == MediaType.MOVIE) {
            if (year == 0) {
                return generalSearchRequest(customQuery, mediaQueryService.getReferenceQuery(),
                        mediaIdentifier, DEFAULT_YEAR_VALUE, SearchType.TMBD_MULTI_SEARCH);
            }
            return generalSearchRequest(customQuery, referenceQuery,
                    mediaIdentifier, year, SearchType.TMDB_API);
        }
        return generalSearchRequest(customQuery, referenceQuery,
                mediaIdentifier, year, SearchType.TMDB_API_TV_YEAR);
    }

    @Override
    public List<QueryResult> searchWithImdbId(String imdbLink,
                                              MediaIdentifier mediaIdentifier,
                                              MediaQueryService mediaQueryService)
            throws NetworkException, MissingReferenceMediaQueryException {
        String imdbId = TextExtractTools.getImdbIdFromLink(imdbLink);
        LOG.info("[ search_with_imdb_id ] {}", imdbId);
        return generalSearchRequest(imdbId, mediaQueryService.getReferenceQuery(),
                mediaIdentifier, DEFAULT_YEAR_VALUE, SearchType.TMDB_API_IMDB_ID);
    }

    @Override
    public List<QueryResult> multiSearchTmdb(
            String customQuery,
            MediaIdentifier mediaIdentifier,
            MediaQueryService mediaQueryService
    ) throws NetworkException, MissingReferenceMediaQueryException {
        LOG.info("[ multi_search ] {}", customQuery);
        return generalSearchRequest(customQuery, mediaQueryService.getReferenceQuery(),
                mediaIdentifier, DEFAULT_YEAR_VALUE, SearchType.TMBD_MULTI_SEARCH);
    }

    /*
     * General request method. Performs search based on search type enum.
     * If any of requests throws exception, an error information is returned as QueryResult object to client.
     * */
    List<QueryResult> generalSearchRequest(String customQuery, MediaQuery mediaQuery,
                                           MediaIdentifier mediaIdentifier, int year, SearchType searchType)
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
                    searchResultsJson = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentifier);
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
                case TMBD_MULTI_SEARCH:
                    searchResultsJson = requestService.tmdbMultiSearch(query); // web search with phrase
                    return responseParser.parseMultiSearchResults(searchResultsJson, filePath);
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
            String responseJson = requestService.tmdbApiRequestWithSpecifiedId(queryResult, MediaIdentifier.IMDB);
            List<QueryResult> queryResults = responseParser.parseTmdbApiWithImdbId(responseJson, queryResult);
            queryResult = queryResults.stream().findFirst().orElse(null);
        }
        if (queryResult != null) {
            LOG.info("[ episode_list ] Found matching episodes.");
            String response = requestService.tmdbGetSeasonDetails(queryResult);
            queryResult = responseParser.parseTvDetail(queryResult, response, seasonNumber);
            // if episodecount == 0 throw ex for incorrect season
        }
//        setCurrentQueryResult(queryResult);
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
                    MediaIdentifier.IMDB,
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

    @Override
    @SuppressWarnings("unchecked")
    public List<QueryResult> getLatestMediaQueryRequest() {
        return getFromCache(LAST_REQUEST_KEY, List.class);
    }

    @Override
    public void setLatestMediaQueryRequest(List<QueryResult> latestMediaQueryRequest) {
        updateCache(LAST_REQUEST_KEY, latestMediaQueryRequest);
    }

    private void setErrorLinks(List<MediaLink> errorLinks) {
        updateCache(ERROR_LINKS_KEY, errorLinks);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MediaLink> getErrorLinks() {
        List<MediaLink> fromCache = getFromCache(ERROR_LINKS_KEY, List.class);
        return (fromCache == null) ? List.of() : fromCache;
    }

    private void clearErrorLinks() {
        clearCache(ERROR_LINKS_KEY);
    }

    private void setDuplicateLinks(List<MediaLink> duplicateLinks) {
        updateCache(DUPLICATE_LINKS_KEY, duplicateLinks);
    }

    private void removeFromDuplicateLinks(MediaLink mediaLink) {
        List<MediaLink> list = getDuplicateLinks()
                .stream()
                .filter(ml -> !ml.getOriginalPath().equals(mediaLink.getOriginalPath()))
                .toList();
        setDuplicateLinks(list);
    }

    private void clearDuplicateLinks() {
        clearCache(DUPLICATE_LINKS_KEY);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MediaLink> getDuplicateLinks() {
        return getFromCache(DUPLICATE_LINKS_KEY, List.class);
    }

    @Override
    public DuplicateMediaLinkDto getNextDuplicateDto() {
        if (getDuplicateLinks().isEmpty()) {
            return null;
        }
        MediaLink duplicateMediaLink = getDuplicateLinks()
                .stream()
                .findFirst()
                .orElse(null);
        MediaLink existingMediaLink =
                duplicateResolverService.linkRecordExist(getMediaLinks(), duplicateMediaLink);
        return (duplicateMediaLink != null)
                ? duplicateResolverService.getDuplicateDto(existingMediaLink, duplicateMediaLink)
                : null;
    }

    @Override
    public void setDuplicateLinkToProcess(DuplicateMediaLinkDto duplicateDto) {
        MediaLink duplicateMediaLink = getDuplicateLinks()
                .stream()
                .filter(ml -> ml.getOriginalPath().equals(duplicateDto.getNewSourcePath()))
                .findFirst()
                .orElse(null);
        if (duplicateMediaLink != null) {
            String newLinkPath = Path
                    .of(duplicateDto.getExistingParentFolder())
                    .resolve(duplicateDto.getNewLinkFileName())
                    .toString();
            removeFromDuplicateLinks(duplicateMediaLink); // TODO
            duplicateMediaLink.setLinkPath(newLinkPath);
            addMediaLinkToProcess(duplicateMediaLink);
            LOG.info("[ link ] Changed link for duplicate");
        }
    }

    @Override
    public void resetProcessLists() {
        clearMediaLinksToProcess();
        clearDuplicateLinks();
        LOG.info("[ duplicate_resolver ] Process lists cleared");
    }

    @Override
    public List<MediaLink> createFileLinks(QueryResult queryResult,
                                           MediaIdentifier mediaIdentifier,
                                           MediaQueryService mediaQueryService)
            throws NetworkException {
        List<MediaLink> mediaLinksToProcess = new LinkedList<>();
        List<MediaLink> duplicateLinks = new LinkedList<>();
        for (MediaQuery mq : mediaQueryService.getProcessList()) {
            MediaLink mediaLink = createFileLink(queryResult, mediaIdentifier, mq, mediaQueryService);
            // if some data needed to create filename is missing, skip it
            if (mediaLink != null) {
                MediaLink existingMediaLink =
                        duplicateResolverService.linkRecordExist(getMediaLinks(), mediaLink);
                if (existingMediaLink != null) {
                    duplicateLinks.add(mediaLink);
                } else mediaLinksToProcess.add(mediaLink);
            }
        }
        setDuplicateLinks(duplicateLinks);
        return mediaLinksToProcess;
    }

    MediaLink createFileLink(QueryResult queryResult,
                             MediaIdentifier mediaIdentifier,
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
    public List<MediaLink> persistsCollectedMediaLinks(MediaQueryService mediaQueryService) {
        int links = 0;
        List<MediaLink> errorMediaLinks = new LinkedList<>();
        for (MediaLink ml : getMediaLinksToProcess()) {
            try {
                Validator.validateForNulls(ml);
//                mediaTrackerDao.addNewLink(ml);
                if (createHardLinkWithDirectories(ml)) {
                    mediaTrackerDao.addNewLink(ml); // TODO moved
                    mediaQueryService.removeQueryByFilePath(ml.getOriginalPath());
                    links++;
                }
            } catch (RequiredFieldException | IllegalAccessException e) {
                LOG.error("[ media_links_service ] Empty media link field: {}", e.getMessage());
            } catch (IOException e) {
                errorMediaLinks.add(ml);
                LOG.error("[ media_links_service ] Error creating link: {}", e.getMessage());
            }
        }
        clearMediaLinksToProcess();
//        clearDuplicateLinks();
        setChangedLinksCount(links);
        setErrorLinks(errorMediaLinks);
        return errorMediaLinks;
    }

    MediaLink createLinkPath(QueryResult queryResult)
            throws RequiredFieldException, IllegalAccessException {
        Path originalPath = Path.of(queryResult.getOriginalPath());
        Path linkPath = Path.of("");
//        fileService.setLinksRootFolder(propertiesService.getLinksFolderMovie());
        if (queryResult.getMediaType() == MediaType.MOVIE) {
            linkPath = fileService.createMovieLinkPath(
                    queryResult,
                    linkIdentifier,
                    propertiesService.getLinksFolderMovie()
            );
        }
        if (queryResult.getMediaType() == MediaType.EXTRAS) {
            linkPath = fileService.createMovieLinkPath(
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

    QueryResult getSelectionDetails(QueryResult queryResult, MediaIdentifier mediaIdentifier)
            throws NetworkException {
        String response = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentifier);
        LOG.info("[ link ] extracting json data...");
        LOG.info("[ link ] {}", queryResult);
        if (mediaIdentifier == MediaIdentifier.TMDB) {
            queryResult = responseParser.parseDetailsRequestByTmdbId(queryResult, response);
        }
        if (mediaIdentifier == MediaIdentifier.IMDB) {
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
            throws IOException, SecurityException {
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
    public int ignoreMediaFile(MediaQueryService mediaQueryService)
            throws MissingReferenceMediaQueryException {
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
        setChangedIgnoreCount(1);
        return 1;
//        return mediaTrackerDao.getMediaLinkByTargetPath(mediaQuery.getFilePath());
    }

    @Override
    public List<MediaLink> getMediaLinks() {
        return filterMediaLinks(false).collect(Collectors.toList());
    }

    @Override
    public List<MediaLink> getMediaIgnoredList() {
        return filterMediaLinks(true).collect(Collectors.toList());
    }

    private Stream<MediaLink> filterMediaLinks(boolean ignoredOnly) {
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
    public List<MediaLink> removeInvalidIgnoreAndLinks() {
        List<MediaLink> deletedLinks = clearInvalidLinks();
        List<MediaLink> deletedIgnore = clearInvalidIgnore();
        setChangedIgnoreCount(-deletedIgnore.size());
        setChangedLinksCount(-deletedLinks.size());
        // change to mutable here
//        deletedLinks
//                .addAll(deletedIgnore);
        return deletedLinks;
    }

    void setInvalidLinksForDeletion(List<MediaLink> invalidLinks) {
        updateCache(INVALID_LINKS_KEY, invalidLinks);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MediaLink> getInvalidLinksForDeletion() {
        return getFromCache(INVALID_LINKS_KEY, List.class);
    }

    void setInvalidIgnoreForDeletion(List<MediaLink> invalidIgnore) {
        updateCache(INVALID_IGNORE_KEY, invalidIgnore);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MediaLink> getInvalidIgnoreForDeletion() {
        return getFromCache(INVALID_IGNORE_KEY, List.class);
    }

    @Override
    public void clearInvalidLists() {
        clearCache(INVALID_LINKS_KEY);
        clearCache(INVALID_IGNORE_KEY);
    }

    @Override
    public void findInvalidElements() {
        findInvalidLinks();
        findInvalidIgnore();
    }

    @Override
    public void findInvalidLinks() {
        LOG.info("Invalid media:");
        List<MediaLink> list = getMediaLinks()
                .stream()
                .filter(ml -> !ml.isOriginalPresent())
                .filter(ml -> !fileService.doesPathExist(ml.getLinkPath()))
                .peek(e -> LOG.info("\t {} -> {}", e.getOriginalPath(), e.getLinkPath()))
                .toList();
        setInvalidLinksForDeletion(list);
    }

    @Override
    public void findInvalidIgnore() {
        LOG.info("Invalid ignored media:");
        List<MediaLink> list = getMediaIgnoredList()
                .stream()
                .filter(mi -> !mi.isOriginalPresent())
                .peek(e -> LOG.info("\t {}", e.getOriginalPath()))
                .toList();
        setInvalidIgnoreForDeletion(list);
    }

    /*
     * Remove link record if neither original nor link file exists.
     * Returns list of removed media links
     * */
    List<MediaLink> clearInvalidLinks() {
//        return getMediaLinks().stream()
        LOG.info("Removing invalid links:");
        return getInvalidLinksForDeletion()
                .stream()
                .peek(ml -> {
                    mediaTrackerDao.removeLink(ml.getMediaId());
                    LOG.info("\t {} -> {}", ml.getOriginalPath(), ml.getLinkPath());
                })
                .toList();
    }

    List<MediaLink> clearInvalidIgnore() {
        LOG.info("Removing invalid ignored media");
//        return getMediaIgnoredList().stream()
        return getInvalidIgnoreForDeletion()
                .stream()
                .peek(mi -> {
                    mediaTrackerDao.removeLink(mi.getMediaId());
                    LOG.info("\t {}", mi.getOriginalPath());
                })
                .toList();
    }

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
        setChangedLinksCount(-1);
        LOG.info("[ remove_link ] Link removed for file: {}", mediaLink.getOriginalPath());
    }

    @Override
    public MediaLink deleteOriginalFile(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        cleanerService.deleteSingleFile(Path.of(mediaLink.getOriginalPath()));
        cleanerService.clearEmptyFolders(Path.of(mediaLink.getOriginalPath()).getParent());
        LOG.info("[ delete_original ] Original file deleted: {}", mediaLink.getOriginalPath());
        mediaLink.setOriginalPresent(false);
        return mediaTrackerDao.addNewLink(mediaLink);
//        mediaTrackerDao.updateLink(mediaLink);
//        return mediaLink;
    }

    @Override
    public MediaLink restoreOriginalFile(long mediaLinkId) throws IOException {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        createHardLinkWithDirectories(Path.of(mediaLink.getOriginalPath()), Path.of(mediaLink.getLinkPath()));
        LOG.info("[ restore_original ] Original file restored: {}", mediaLink.getOriginalPath());
        mediaLink.setOriginalPresent(true);
        return mediaTrackerDao.addNewLink(mediaLink);
//        mediaTrackerDao.updateLink(mediaLink);
//        return mediaLink;
    }

    @Override
    public void undoIgnoreMedia(long mediaIgnoreId) {
        mediaTrackerDao.removeLink(mediaIgnoreId);
        setChangedIgnoreCount(-1);
        LOG.info("[ remove_link ] Link removed for file: {}", mediaIgnoreId);

    }

    /*
     * Checks if original path of given media link exists and updates boolean value if needed.
     * */
    MediaLink validateLink(MediaLink mediaLink) {
        boolean originalPresent = mediaLink.isOriginalPresent();
        if (
                fileService.doesPathExist(mediaLink.getOriginalPath())
                        != originalPresent
        ) {
            mediaLink.setOriginalPresent(!originalPresent);
            return mediaTrackerDao.addNewLink(mediaLink);
        }
//            if (!fileService.doesPathExist(mediaLink.getOriginalPath())
//                    && mediaLink.isOriginalPresent()) {
//                mediaLink.setOriginalPresent(false);
//                mediaTrackerDao.updateLink(mediaLink);
//            }
        return mediaLink;
    }

    private void setFoldersForClearing(List<Path> paths) {
        updateCache(EMPTY_FOLDERS, paths);
    }

    private void clearFoldersForClearing() {
        clearCache(EMPTY_FOLDERS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Path> getFoldersForClearing() {
        return getFromCache(EMPTY_FOLDERS, List.class);
    }

    @Override
    public void abortFolderClearing() {
        clearFoldersForClearing();
    }

    @Override
    public boolean findEmptyFolders(MediaType mediaType) throws IOException {
        List<Path> rootPaths = (mediaType == MediaType.MOVIE)
                ? propertiesService.getSourceFolderListMovie()
                : propertiesService.getSourceFolderListTv();
        if (!rootPaths.isEmpty()) {
            System.out.println(rootPaths);
            List<Path> output = new LinkedList<>();
            for (Path p : rootPaths) {
                try (Stream<Path> rootContent = Files.list(p)) {
                    rootContent
                            .filter(Files::isDirectory)
                            .filter(cleanerService::containsNoMediaFiles)
                            .forEach(output::add);
                }
                // TODO check for nested folders
//                if (Files.isDirectory(p) && cleanerService.containsNoMediaFiles(p)) {
//
//                    output.add(p);
//                }
            }
//            List<Path> foundPaths = cleanerService.checkPathForClearing(rootPaths);
            if (!output.isEmpty()) {
                setFoldersForClearing(output);
                return true;
            }
        }
        return false;
    }

    @Override
    public void persistRemoveEmptyFolders() {
        for (Path path : getFoldersForClearing()) {
            cleanerService.clearEmptyFolders(path);
        }
    }

    /*
     * TODO change creating links to moving files
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
                MediaLink mediaLink = mediaTrackerDao.addNewLink(ml);
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

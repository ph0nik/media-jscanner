package service;

import com.google.gson.JsonParseException;
import dao.MediaTrackerDao;
import model.*;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import util.*;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MediaLinksServiceImpl extends PaginationImpl implements MediaLinksService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaLinksServiceImpl.class);
    private static final String WHITESPACE = " ";
    private static final MediaIdentity LINK_IDENTIFIER = MediaIdentity.IMDB;
    private static final int DEFAULT_YEAR_VALUE = 0; // 1000
    private Path linksFolder;
    private int queryListSize = 0;
    private LastRequest lastRequest = null;
    //    private MediaTrackerDao mediaTrackerDao;
//    private CleanerService cleanerService;
//    private MediaQueryService mediaQueryService;
//    private PropertiesService propertiesService;
//    private FileService fileService;
//    private ResponseParser responseParser;
//    private RequestService requestService;
//
//    @Autowired
//    public MediaLinksServiceImpl(@Qualifier("spring") MediaTrackerDao dao, PropertiesService propertiesService,
//                                 CleanerService cleanerService, MediaQueryService mediaQueryService, FileService fs) {
////        super(dao, mediaQueryService);
//        this.fileService = fs;
////        this.queryListSize = 0;
//        this.mediaQueryService = mediaQueryService;
//        this.cleanerService = cleanerService;
//        this.propertiesService = propertiesService;
//        refreshUserPaths();
//        mediaTrackerDao = dao;
////        lastRequest = null;
//        responseParser = ResponseParser.getResponseParser(propertiesService.getNetworkProperties());
//        requestService = RequestService.getRequestService(propertiesService.getNetworkProperties());
//    }
    @Autowired
    @Qualifier("spring")
    private MediaTrackerDao mediaTrackerDao;
    @Autowired
    private CleanerService cleanerService;
    @Autowired
    private MediaQueryService mediaQueryService;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private FileService fileService;
    @Autowired
    private ResponseParser responseParser;
    @Autowired
    private RequestService requestService;

    public MediaLinksServiceImpl() {

    }

    @PostConstruct
    void refreshUserPaths() {
        linksFolder = propertiesService.getLinksFolder();
//        responseParser = ResponseParser.getResponseParser(propertiesService.getNetworkProperties());
//        requestService = RequestService.getRequestService(propertiesService.getNetworkProperties());
    }

    @Override
    public List<MediaQuery> getMediaQueryList() {
        queryListSize = mediaQueryService.getCurrentMediaQueries().size();
        return mediaQueryService.getCurrentMediaQueries();
    }

    @Override
    public int getMediaQueryListSize() {
        return queryListSize;
    }

    /*
     * Executes media query search using web search engine and web api search engine.
     * Return results or empty list if nothing was found.
     * On connection error it returns query result elements with error description.
     * */
    @Override
    public List<QueryResult> executeMediaQuery(String customQuery, MediaIdentity mediaIdentity) {
        MediaQuery mediaQuery = mediaQueryService.getReferenceQuery();
        // TODO make guards so that malformed input gets correct response
        List<QueryResult> webSearchResults = generalSearchRequest(customQuery, mediaQuery, mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.WEB_SEARCH);
        List<QueryResult> tmdbSearchResults = generalSearchRequest(customQuery, mediaQuery, mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.TMDB_API);
        tmdbSearchResults.addAll(webSearchResults);
        lastRequest = new LastRequest(tmdbSearchResults, mediaQuery);
        return tmdbSearchResults;
    }

    @Override
    public List<QueryResult> searchTmdbWithTitleAndYear(String customQuery, MediaIdentity mediaIdentity, int year) {
        LOG.info("[ search_with_year ] {} | {} | {}", customQuery, mediaIdentity, year);
        return generalSearchRequest(customQuery, mediaQueryService.getReferenceQuery(),
                mediaIdentity, year, SearchType.TMDB_API);
    }

    @Override
    public List<QueryResult> searchWithImdbId(String imdbLink, MediaIdentity mediaIdentity) {
        String imdbId = TextExtractTools.getImdbIdFromLink(imdbLink);
//        return generalSearchRequest(imdbId, mediaQueryService.getReferenceQuery(),
//                mediaIdentity, 1000, SearchType.TMDB_API_IMDB_ID);
        return generalSearchRequest(imdbId, mediaQueryService.getReferenceQuery(),
                mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.TMDB_API_IMDB_ID);
    }

    /*
     * General request method. Performs search based on search type enum.
     * If any of requests throws exception, an error information is returned as QueryResult object to client.
     * */
    List<QueryResult> generalSearchRequest(String customQuery, MediaQuery mediaQuery,
                                           MediaIdentity mediaIdentity, int year, SearchType searchType) {
//        List<QueryResult> queryResults = new ArrayList<>();
        Path filePath = Path.of(mediaQuery.getFilePath());
        String query = (customQuery.isEmpty()) ? mediaQuery.getQuery() : customQuery;
        String searchResults;
        try {
            if (searchType == SearchType.TMDB_API) {
                searchResults = requestService.tmdbApiTitleAndYear(
                        new DeductedQuery(query, String.valueOf(year), mediaQuery.getFilePath())
                );
                // TODO multisearch results for tmdb request
//                searchResults = requestService.tmdbMultiSearch(deductedQuery);
//                queryResults = responseParser.parseTmdbApiSearchResults(searchResults, filePath);
                return responseParser.parseTmdbApiSearchResults(searchResults, mediaQuery.getFilePath());
            }
            if (searchType == SearchType.TMDB_API_IMDB_ID) {
                QueryResult queryResult = new QueryResult(mediaQuery.getFilePath());
                queryResult.setImdbId(customQuery);
                searchResults = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentity);
                return List.of(responseParser.parseTmdbApiWithImdbId(searchResults, queryResult));
//                queryResults.add(responseParser.parseTmdbApiWithImdbId(searchResults, queryResult));
            }
            searchResults = requestService.webSearchRequest(query);
            return responseParser.parseWebSearchResults(searchResults, mediaQuery.getFilePath(), mediaIdentity);
        } catch (HttpStatusException e) {
            QueryResult errorQuery = createErrorResponse(mediaQuery.getFilePath(),
                    e.getUrl(),
                    "Connection error: " + e.getStatusCode());
            LOG.error("[ general_search ] Connection error: {} @ {}", e.getStatusCode(), e.getUrl());
//            queryResults.add(errorQuery);
            return List.of(errorQuery);
        } catch (UnknownHostException e) {
            String errorMessage = "[ general_search ] Host not found: " + e.getMessage();
            QueryResult errorQuery = createErrorResponse(mediaQuery.getFilePath(),
                    e.getMessage(),
                    errorMessage);
            LOG.error(errorMessage);
//            queryResults.add(errorQuery);
            return List.of(errorQuery);
        } catch (IOException e) {
            String errorMessage = "[ general_search ] IO Exception: " + e.getMessage();
            QueryResult errorQuery = createErrorResponse(mediaQuery.getFilePath(),
                    e.getMessage(),
                    errorMessage);
            LOG.error(errorMessage);
//            queryResults.add(errorQuery);
            return List.of(errorQuery);
        } finally {
            searchResults = null;
        }
//        return queryResults;
    }

    QueryResult createErrorResponse(String originalPath, String errorMessage, String errorDescription) {
        QueryResult errorQuery = new QueryResult(originalPath);
        errorQuery = fillNullEntries(errorQuery);
        errorQuery.setTheMovieDbId(-1);
        errorQuery.setTitle(errorMessage);
        errorQuery.setDescription(errorDescription);
        return errorQuery;
    }

    /*
     * Checks for every nullable object parameter and fills it with empty string
     * */
    QueryResult fillNullEntries(QueryResult queryResult) {
        if (queryResult.getUrl() == null) queryResult.setUrl("");
        if (queryResult.getOriginalPath() == null) queryResult.setOriginalPath("");
        if (queryResult.getImdbId() == null) queryResult.setImdbId("");
        if (queryResult.getDescription() == null) queryResult.setDescription("");
        if (queryResult.getTitle() == null) queryResult.setTitle("");
        if (queryResult.getPoster() == null) queryResult.setPoster("");
        if (queryResult.getYear() == null) queryResult.setYear("");
        return queryResult;
    }

    @Override
    public LastRequest getLatestMediaQueryRequest() {
        return lastRequest;
    }

    @Override
    public List<LinkCreationResult> createFileLink(QueryResult queryResult, MediaIdentity mediaIdentity) {
        return mediaQueryService.getProcessList()
                .stream()
                .map(mq -> createFileLink(queryResult, mediaIdentity, mq))
                .collect(Collectors.toList());
    }

    // TODO separate elements of the process, get the data and return list of medialinks with given
    // results, then process list and create actual links
    LinkCreationResult createFileLink(QueryResult queryResult, MediaIdentity mediaIdentifier, MediaQuery mediaQuery) {
        // naming pattern -> Film (2018) [tmdbid-65567]
        // send request to themoviedb api with given query result
        refreshUserPaths();
        // TODO temporary, probably merge query result and media transfer data objects
//        queryResult.setOriginalPath(mediaQuery.getFilePath());
        MediaTransferData mediaTransferData = new MediaTransferData();
        mediaTransferData.setMediaType(mediaQuery.getMediaType());
        mediaTransferData.setPartNumber(mediaQuery.getMultipart());
        LinkCreationResult linkCreationResult;
        mediaTransferData = getSelectionDetails(mediaTransferData, queryResult, mediaIdentifier);
        try {
            // TODO break here into multiple modules
            MediaLink mediaLink = createFilePaths(queryResult, mediaTransferData, new MediaLink());
            linkCreationResult = createHardLinkWithDirectories(mediaLink);
            if (linkCreationResult.isCreationStatus() && !linkRecordExist(mediaLink)) {
                mediaTrackerDao.addNewLink(mediaLink);
                mediaQueryService.removeQueryByFilePath(queryResult.getOriginalPath());
            }
        } catch (FileNotFoundException e) {
            String errorMessage = "[ file_service ] No links root folder defined.";
            LOG.error(errorMessage);
            linkCreationResult = new LinkCreationResult(false, errorMessage, new MediaLink());
        }
        return linkCreationResult;
    }

    boolean linkRecordExist(MediaLink mediaLink) {
        return getMediaLinks().stream()
                .anyMatch(ml ->
                        ml.getLinkPath().equals(mediaLink.getLinkPath()) &&
                                ml.getOriginalPath().equals(mediaLink.getOriginalPath()));
    }

    MediaLink createFilePaths(QueryResult queryResult, MediaTransferData mediaTransferData, MediaLink mediaLink) throws FileNotFoundException {
        Path originalPath = Path.of(queryResult.getOriginalPath());
        Path linkPath = Path.of("");
        fileService.setLinksRootFolder(linksFolder);
        if (mediaTransferData.getMediaType() == MediaType.MOVIE) {
            linkPath = fileService.createMovieLinkPath(queryResult, mediaTransferData, LINK_IDENTIFIER);
        }
        if (mediaTransferData.getMediaType() == MediaType.EXTRAS) {
            linkPath = fileService.createExtrasLinkPath(queryResult, mediaTransferData, LINK_IDENTIFIER);
        }
        mediaLink.setOriginalPath(originalPath.toString());
        mediaLink.setLinkPath(linkPath.toString());
        mediaLink.setTheMovieDbId(mediaTransferData.getTmdbId());
        mediaLink.setImdbId(mediaTransferData.getImdbId());
        return mediaLink;
    }

    MediaTransferData getSelectionDetails(MediaTransferData mediaTransferData, QueryResult queryResult, MediaIdentity mediaIdentifier) {
        MediaLink mediaLink = new MediaLink();
        String response;
        try {
            response = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentifier);
            LOG.info("[ link ] extracting json data...");
            LOG.info("[ link ] {}", queryResult);
            if (mediaIdentifier == MediaIdentity.TMDB) {
                mediaTransferData = responseParser.parseDetailsRequestByTmdbId(mediaTransferData, response);
                mediaTransferData.setTmdbId(queryResult.getTheMovieDbId());
            }
            if (mediaIdentifier == MediaIdentity.IMDB) {
                mediaTransferData = responseParser.parseDetailsRequestByExternalId(mediaTransferData, response);
                mediaTransferData.setImdbId(queryResult.getImdbId());
            }
        } catch (HttpStatusException e) {
            LOG.error("[ link ] {}", e.getMessage());
            String message = e.getStatusCode() + " : " + e.getMessage();
            mediaTransferData.setLinkCreationResult(new LinkCreationResult(false, message, mediaLink));
        } catch (IOException | JsonParseException e) {
            LOG.error("[ link ] {}", e.getMessage());
            mediaTransferData.setLinkCreationResult(new LinkCreationResult(false, e.getMessage(), mediaLink));
        } finally {
            response = null;
        }
        if (mediaTransferData.getTitle() == null || mediaTransferData.getTitle().isEmpty()) {
            String resultMessage = "Unable to create link, MediaData object is empty";
            LOG.error("[ link ] {}", resultMessage);
            mediaTransferData.setLinkCreationResult(new LinkCreationResult(false, resultMessage, mediaLink));
        }
        return mediaTransferData;
    }


    LinkCreationResult createHardLinkWithDirectories(MediaLink mediaLink) {
        return createHardLinkWithDirectories(mediaLink, false);
    }

    /*
     * Creates hard link with parameters provided in MediaLink object.
     * Returns LinkCreationResult which contains result status (true for success and false for failure),
     * optional error message and original MediaLink object.
     * Params:   mediaLink - object containing prerequisites for creating link
     *           existingLink - boolean value representing current state of link, for existing links
     *           use true to invert and recreate original, source file.
     * */
    LinkCreationResult createHardLinkWithDirectories(MediaLink mediaLink, boolean existingLink) {
        LOG.info("[ link ] {}", mediaLink);
        Path linkPath = (existingLink) ? Path.of(mediaLink.getOriginalPath()) : Path.of(mediaLink.getLinkPath());
        Path parentLinkPath = linkPath.getRoot().resolve(linkPath.subpath(0, linkPath.getNameCount() - 1));
        try {
            if (!Files.exists(parentLinkPath)) {
                Files.createDirectories(parentLinkPath);
                LOG.info("[ link ] creating folder...: {}", parentLinkPath);
            }
            LOG.info("[ link ] creating link...");
            Path target = (existingLink) ? Path.of(mediaLink.getLinkPath()) : Path.of(mediaLink.getOriginalPath());
            Path link = (existingLink) ? Path.of(mediaLink.getOriginalPath()) : Path.of(mediaLink.getLinkPath());
            Files.createLink(link, target);
            LOG.info("[ link ] link created: {} => {}", mediaLink.getLinkPath(), mediaLink.getOriginalPath());
            return new LinkCreationResult(true, "New link added", mediaLink);
        } catch (FileAlreadyExistsException e) {
            LOG.error("[ link ] Link already exists: {}", e.getMessage());
            return new LinkCreationResult(true, "File already exists: " + e.getMessage(), mediaLink);
        } catch (IOException | SecurityException e) {
            LOG.error(e.getMessage());
            return new LinkCreationResult(false, e.getMessage(), mediaLink);
        }
    }

    @Override
    public MediaLink ignoreMediaFile() {
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

    boolean linkContains(MediaLink mediaLink, String[] strings) {
        return Arrays.stream(strings).allMatch(str ->
                mediaLink.getOriginalPath().toLowerCase().contains(str) ||
                        mediaLink.getLinkPath().toLowerCase().contains(str)
        );
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
        getMediaLinks().stream()
                .filter(ml -> !ml.isOriginalPresent())
                .filter(ml -> !validatePath(ml.getLinkPath()))
                .forEach(ml -> mediaTrackerDao.removeLink(ml.getMediaId()));
    }

    @Override
    public MediaQuery moveBackToQueue(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.removeLink(mediaLinkId);
        Path linkPath = Path.of(mediaLink.getLinkPath());
        // delete file
        cleanerService.deleteElement(linkPath);
        // remove folder if possible
//        if (cleanerService.containsNoMediaFiles(linkPath.getParent())) cleanerService.deleteElement(linkPath.getParent());
        MediaQuery mediaQuery = mediaQueryService.addQueryToQueue(mediaLink.getOriginalPath());
        LOG.info("[ remove_link ] Link removed for file: {}", mediaLink.getOriginalPath());
        return mediaQuery;
    }

    @Override
    public MediaLink deleteOriginalFile(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        cleanerService.deleteElement(Path.of(mediaLink.getOriginalPath()));
        cleanerService.clearParentFolder(Path.of(mediaLink.getOriginalPath()).getParent());
        LOG.info("[ delete_original ] Original file deleted: {}", mediaLink.getOriginalPath());
        mediaLink.setOriginalPresent(false);
        mediaTrackerDao.updateLink(mediaLink);
        return mediaLink;
    }

    @Override
    public MediaLink restoreOriginalFile(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        createHardLinkWithDirectories(mediaLink, true);
        LOG.info("[ restore_original ] Original file restored: {}", mediaLink.getOriginalPath());
        mediaLink.setOriginalPresent(true);
        mediaTrackerDao.updateLink(mediaLink);
        return mediaLink;
    }

    @Override
    public MediaQuery unIgnoreMedia(long mediaIgnoreId) {
        MediaLink mediaLink = mediaTrackerDao.removeLink(mediaIgnoreId);
        MediaQuery mediaQuery = mediaQueryService.addQueryToQueue(mediaLink.getOriginalPath());
        LOG.info("[ remove_link ] Link removed for file: {}", mediaLink.getOriginalPath());
        return mediaQuery;
    }

    /*
     * Checks if original path of given media link exists and updates boolean value if needed.
     * */
    MediaLink validateLink(MediaLink mediaLink) {
        if (!validatePath(mediaLink.getOriginalPath()) && mediaLink.isOriginalPresent()) {
            mediaLink.setOriginalPresent(false);
            mediaTrackerDao.updateLink(mediaLink);
        }
        return mediaLink;
    }

    /*
     * Checks whether given path of file or directory exists
     * */
    @Override
    public boolean validatePath(String path) {
        return Path.of(path).toFile().exists();
    }

    // test
    @Override
    public void removeEmptyFolders() {
        propertiesService.getTargetFolderList().forEach(cleanerService::clearEmptyFolders);
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
                cleanerService.clearParentFolder(oldLinkPath);
                LOG.info("[ link ] Link moved to a new folder: {}", mediaLink);
            } catch (FileAlreadyExistsException e) {
                LOG.error("[ link ] Link already exists: {}", e.getMessage());
                new LinkCreationResult(false, e.getMessage(), ml);
            } catch (IOException | SecurityException e) {
                LOG.error(e.getMessage());
                new LinkCreationResult(false, e.getMessage(), ml);
            }
        }
    }

    /*
     * Returns true if more than one media file belongs to the same directory at the same level
     * */
    @Override
    public boolean isMultipart(MediaQuery mediaQuery) {
        return mediaQueryService.getGroupedQueries(mediaQuery.getQueryUuid()).size() > 1;
    }

}

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

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static util.TextExtractTools.*;

@Component
public class MediaLinksServiceImpl extends PaginationImpl implements MediaLinksService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaLinksServiceImpl.class);
    private static final String LINKS_ROOT = "LINKS_ROOT";
    private static final MediaIdentity LINK_IDENTIFIER = MediaIdentity.IMDB;
    private static final int DEFAULT_YEAR_VALUE = 1000;
    private final Path linksFolder;
    private final MediaTrackerDao mediaTrackerDao;
    private final CleanerService cleanerService;
    private final ResponseParser responseParser;
    private final RequestService requestService;
    private final MediaQueryService mediaQueryService;
    private LastRequest lastRequest;

//    @Autowired
//    public MediaLinksServiceImpl() {
//        super(mediaTrackerDao, mediaQueryService);
//    }
    @Autowired
    public MediaLinksServiceImpl(@Qualifier("spring") MediaTrackerDao dao, PropertiesService propertiesService,
                                 CleanerService cleanerService, MediaQueryService mediaQueryService) {
//        super(dao, mediaQueryService);
        this.mediaQueryService = mediaQueryService;
        this.cleanerService = cleanerService;
        linksFolder = propertiesService.getLinksFolder();
        mediaTrackerDao = dao;
        lastRequest = null;
        responseParser = ResponseParser.getResponseParser(propertiesService.getNetworkProperties());
        requestService = RequestService.getRequestService(propertiesService.getNetworkProperties());
    }


    @Override
    public List<MediaQuery> getMediaQueryList() {
        return mediaQueryService.getCurrentMediaQueries();
//        return mediaTrackerDao.getAllMediaQueries();
    }

    /*
     * Executes media query search using web search engine and web api search engine.
     * Return results or empty list if nothing was found.
     * On connection error it returns query result elements with error description.
     * */
    @Override
    public List<QueryResult> executeMediaQuery(String customQuery, MediaQuery mediaQuery, MediaIdentity mediaIdentity) {
        List<QueryResult> webSearchResults = generalSearchRequest(customQuery, mediaQuery, mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.WEB_SEARCH);
        List<QueryResult> tmdbSearchResults = generalSearchRequest(customQuery, mediaQuery, mediaIdentity, DEFAULT_YEAR_VALUE, SearchType.TMDB_API);
        tmdbSearchResults.addAll(webSearchResults);
        lastRequest = new LastRequest(tmdbSearchResults, mediaQuery);
        return tmdbSearchResults;
    }

    @Override
    public List<QueryResult> searchTmdbWithTitleAndYear(String customQuery, MediaQuery mediaQuery, MediaIdentity mediaIdentity, int year) {
        return generalSearchRequest(customQuery, mediaQuery, mediaIdentity, year, SearchType.TMDB_API);
    }

    /*
     * General request method. Performs search based on search type enum.
     * If any of requests throws exception, an error information is returned as QueryResult object to client.
     * */
    List<QueryResult> generalSearchRequest(String customQuery, MediaQuery mediaQuery, MediaIdentity mediaIdentity, int year, SearchType searchType) {
        List<QueryResult> queryResults = new ArrayList<>();
        Path filePath = Path.of(mediaQuery.getFilePath());
        String query = (customQuery.isEmpty()) ? mediaQuery.getQuery() : customQuery;
        try {
            if (searchType == SearchType.TMDB_API) {
                DeductedQuery deductedQuery = new DeductedQuery(query, String.valueOf(year), mediaQuery.getFilePath());
                String tmdbSearch = requestService.tmdbApiTitleAndYear(deductedQuery);
                if (!tmdbSearch.isEmpty())
                    queryResults = responseParser.parseTmdbApiSearchResults(tmdbSearch, filePath);
            } else {
                String webSearchResults = requestService.webSearchRequest(query);
                if (!webSearchResults.isEmpty()) {
                    queryResults = responseParser.parseWebSearchResults(webSearchResults, filePath, mediaIdentity);
                }
            }
        } catch (HttpStatusException e) {
            QueryResult errorQuery = new QueryResult();
            errorQuery.setTheMovieDbId(-1);
            errorQuery.setTitle(e.getUrl());
            errorQuery.setDescription("Connection error: " + e.getStatusCode());
            errorQuery = fillNullEntries(errorQuery);
            queryResults.add(errorQuery);
            LOG.error("[ general_search ] Connection error: {} @ {}", e.getStatusCode(), e.getUrl());
        } catch (UnknownHostException e) {
            QueryResult errorQuery = new QueryResult();
            errorQuery.setTheMovieDbId(-1);
            errorQuery.setTitle(e.getMessage());
            errorQuery.setDescription("[ search ] Host not found: " + e.getMessage());
            errorQuery = fillNullEntries(errorQuery);
            queryResults.add(errorQuery);
            LOG.error("[ general_search ] Host not found: {}", e.getMessage());
        } catch (IOException e) {
            LOG.error(e.getMessage());
            QueryResult errorQuery = new QueryResult();
            errorQuery.setTheMovieDbId(-1);
            errorQuery.setTitle(e.getMessage());
            errorQuery.setDescription("[ general_search ] IO Exception");
            errorQuery = fillNullEntries(errorQuery);
            queryResults.add(errorQuery);
        }
        return queryResults;
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
    public LinkCreationResult createSymLink(QueryResult queryResult, MediaIdentity mediaIdentifier, MediaType mediaType) {
        // naming pattern -> Film (2018) [tmdbid-65567]
        // send request to themoviedb api with given query result
        MediaTransferData mediaTransferData = new MediaTransferData();
        mediaTransferData.setMediaType(mediaType);
        MediaLink mediaLink = new MediaLink();
        String resultMessage;
        LinkCreationResult linkCreationResult = null;
        try {
            String response = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentifier);
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
            LOG.error(e.getMessage());
            String message = e.getStatusCode() + " : " + e.getMessage();
            return new LinkCreationResult(false, message, mediaLink);
        } catch (IOException | JsonParseException e) {
            LOG.error(e.getMessage());
            return new LinkCreationResult(false, e.getMessage(), mediaLink);
        }
        if (mediaTransferData.getTitle() == null || mediaTransferData.getTitle().isEmpty()) {
            resultMessage = "Unable to create link, MediaData object is empty";
            LOG.error("[ link ] {}", resultMessage);
            return new LinkCreationResult(false, resultMessage, mediaLink);
        }

        Path originalPath = Path.of(queryResult.getOriginalPath());
        Path linkPath = Path.of("");
        if (mediaType == MediaType.MOVIE) {
            linkPath = createMovieLinkPath(queryResult, mediaTransferData, LINK_IDENTIFIER);
        }
        if (mediaType == MediaType.EXTRAS) {
            linkPath = createExtrasLinkPath(queryResult, mediaTransferData, LINK_IDENTIFIER);
        }
        mediaLink.setOriginalPath(originalPath.toString());
        mediaLink.setLinkPath(linkPath.toString());
        mediaLink.setTheMovieDbId(mediaTransferData.getTmdbId());
        mediaLink.setImdbId(mediaTransferData.getImdbId());

        linkCreationResult = createHardLinkWithDirectories(mediaLink);
        if (linkCreationResult.isCreationStatus()) {
            mediaTrackerDao.addNewLink(mediaLink);
            mediaQueryService.removeQueryByFilePath(queryResult.getOriginalPath());
        }
        return linkCreationResult;
    }

    LinkCreationResult createHardLinkWithDirectories(MediaLink mediaLink) {
        return createHardLinkWithDirectories(mediaLink, false);
    }
    LinkCreationResult createHardLinkWithDirectories(MediaLink mediaLink, boolean existingLink) {
        Path linkPath = Path.of(mediaLink.getLinkPath());
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
            return new LinkCreationResult(false, "File already exists: " + e.getMessage(), mediaLink);
        } catch (IOException | SecurityException e) {
            LOG.error(e.getMessage());
            return new LinkCreationResult(false, e.getMessage(), mediaLink);
        }
    }

    @Override
    public MediaLink ignoreMediaFile(MediaQuery mediaQuery) {
        MediaLink mediaIgnored = new MediaLink();
        mediaIgnored.setOriginalPath(mediaQuery.getFilePath());
        mediaIgnored.setLinkPath("ignore");
        mediaIgnored.setImdbId("none");
        mediaIgnored.setTheMovieDbId(-1);

        mediaTrackerDao.addNewLink(mediaIgnored);
        LOG.info("[ ignore ] Ignored element: {}", mediaQuery.getFilePath());
        mediaQueryService.removeQueryFromQueue(mediaQuery);
        return mediaTrackerDao.findMediaLinkByTargetPath(mediaQuery.getFilePath());
    }

    @Override
    public List<MediaLink> getMediaLinks() {
        return mediaTrackerDao.getAllMediaLinks()
                .stream()
                .filter(ml -> ml.getTheMovieDbId() >= 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<MediaLink> getMediaIgnoredList() {
        List<MediaLink> collect = mediaTrackerDao.getAllMediaLinks()
                .stream()
                .filter(ml -> ml.getTheMovieDbId() < 0)
                .collect(Collectors.toList());
        return collect;
    }

    /*
     * Create file path for symlink file with given query result and media data
     * */
    Path createMovieLinkPath(QueryResult queryResult, MediaTransferData mediaTransferData,
                             MediaIdentity mediaIdentity) {
        // check if movie is divided into multiple parts
        int discNumber = checkForMultiDiscs(queryResult.getOriginalPath());
        String part = (discNumber > 0) ? "cd" + discNumber + " " : "";
        String title = replaceIllegalCharacters(mediaTransferData.getTitle());
        String yearFormatted = " (" + mediaTransferData.getYear() + ")";
        String idFormatted = "";
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + queryResult.getTheMovieDbId() + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + mediaTransferData.getImdbId() + "]";
        }
        String extension = getExtension(queryResult.getOriginalPath());
        // get special identifier for movie extras
        String special = checkForSpecialDescriptor(queryResult.getOriginalPath());
        String group = ""; //getGroupName(queryResult.getOriginalPath());
        String specialWithGroup = (part + " " + special + " " + group).trim();
        specialWithGroup = (specialWithGroup.trim().isEmpty()) ? "" : " - [" + specialWithGroup + "]";
        // build path names
        LOG.info("[ link ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        LOG.info("[ link ] folder: {}", movieFolder);
        String movieName = title + specialWithGroup + "." + extension;
        LOG.info("[ link ] file: {}", movieName);
//        return Path.of(LINKS_ROOT).resolve(movieFolder).resolve(movieName);
        return linksFolder.resolve(movieFolder).resolve(movieName);
    }


    private Path createExtrasLinkPath(QueryResult queryResult, MediaTransferData mediaTransferData,
                                      MediaIdentity mediaIdentity) {
        String title = replaceIllegalCharacters(mediaTransferData.getTitle());

        int year = mediaTransferData.getYear();
        String yearFormatted = " (" + year + ")";

        String imdbId = mediaTransferData.getImdbId();
        int tmdbId = queryResult.getTheMovieDbId();
        String idFormatted = "";
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + tmdbId + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + imdbId + "]";
        }
        Path of = Path.of(queryResult.getOriginalPath());
        Path fileName = of.getName(of.getNameCount() - 1);

        // build path names
        LOG.info("[ link ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        String extrasFolder = "extras";
        String movieName = fileName.toString();
//        return Path.of(LINKS_ROOT).resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
        return linksFolder.resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
    }

    @Override
    public MediaQuery moveBackToQueue(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.removeLink(mediaLinkId);
        Path linkPath = Path.of(mediaLink.getLinkPath());
        cleanerService.deleteElement(linkPath.getParent());
        MediaQuery mediaQuery = mediaQueryService.addQueryToQueue(mediaLink.getOriginalPath());
        LOG.info("[ remove_link ] Link removed for file: {}", mediaLink.getOriginalPath());
        return mediaQuery;
    }

    @Override
    public MediaLink deleteOriginalFile(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        cleanerService.deleteElement(Path.of(mediaLink.getOriginalPath()));
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
    @Transactional
    public MediaQuery unIgnoreMedia(long mediaIgnoreId) {
        MediaLink mediaLink = mediaTrackerDao.removeLink(mediaIgnoreId);
        MediaQuery mediaQuery = mediaQueryService.addQueryToQueue(mediaLink.getOriginalPath());
        LOG.info("[ remove_link ] Link removed for file: {}", mediaLink.getOriginalPath());
        return mediaQuery;
    }

    /*
     * Checks whether given path of file or directory exists
     * */
    @Override
    public boolean validatePath(Path path) {
        return path.toFile().exists();
    }

    /*
     * TODO is this function necessary?
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

    @Override
    public List<MediaLink> searchMediaLinks(String search) {
        return getMediaLinks().stream()
                .filter(ml -> ml.getOriginalPath().toLowerCase().contains(search.toLowerCase())
                        || ml.getLinkPath().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        String pasd = "E:\\Filmy SD\\Memories of Matsuko-Kiraware Matsuko no Isshō\\wrd-matsuko-cd1.avi";
        String regex = "-([a-zA-Z0-9]+)(\\.\\w+)?$";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(pasd);
        String out = "";
        if (matcher.find()) {
            int i = matcher.groupCount();
            if (i > 0) out = matcher.group(1);
        }
        System.out.println(out);
    }
}

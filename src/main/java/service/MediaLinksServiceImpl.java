package service;

import dao.MediaTrackerDao;
import model.MediaData;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import util.CleanerService;
import util.MediaIdentity;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MediaLinksServiceImpl implements MediaLinksService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaLinksServiceImpl.class);

    // TODO cleanup of properties strings, create additional class for some methods
    private final PropertiesService props;
    private final MediaTrackerDao mediaTrackerDao;
    private final Properties networkProperties;
    private final CleanerService cleanerService;
    private final ResponseParser responseParser;
    private final RequestService requestService;

    private List<QueryResult> lastRequest;

    public MediaLinksServiceImpl(MediaTrackerDao dao, PropertiesService propertiesService,
                                 CleanerService cleanerService) {
        this.cleanerService = cleanerService;
        props = propertiesService;
        networkProperties = props.getNetworkProperties();
        mediaTrackerDao = dao;
        lastRequest = null;
        responseParser = new ResponseParser(networkProperties);
        requestService = new RequestService(networkProperties);
    }

    @Override
    public List<MediaQuery> getMediaQueryList() {
        return mediaTrackerDao.getAllMediaQueries();
    }

    /*
     * Executes media query search using web search engine and web api search engine.
     * Return results or empty list if nothing was found.
     * On connection error it returns query result elements with error description.
     * */
    @Override
    public List<QueryResult> executeMediaQuery(String customQuery, MediaQuery mediaQuery, MediaIdentity mediaIdentity) {
        String query = (customQuery.isEmpty()) ? mediaQuery.getQuery() : customQuery;
        List<QueryResult> queryResults = new ArrayList<>();
        Path filePath = Path.of(mediaQuery.getFilePath());
        String webSearchResults = "";
        String tmdbSearch = "";
        try {
            webSearchResults = requestService.searchEngineRequest(query, mediaIdentity);
            // catch any response error
        } catch (HttpStatusException e) {
            /*
            * catch error status code and pass error cause as return object
            * */
            QueryResult errorQuery = new QueryResult();
            errorQuery.setTheMovieDbId(-1);
            errorQuery.setTitle(e.getUrl());
            errorQuery.setDescription("Connection error: " + e.getStatusCode());
            queryResults.add(errorQuery);
            LOG.error("[ search ] Connection error: {} @ {}", e.getStatusCode(), e.getUrl());
        } catch (UnknownHostException e) {
            /*
            * catch network error and pass error code as return object
            * */
            QueryResult errorQuery = new QueryResult();
            errorQuery.setTheMovieDbId(-1);
            errorQuery.setTitle(e.getMessage());
            errorQuery.setDescription("[ search ] Host not found");
            queryResults.add(errorQuery);
            LOG.error("[ search ] Host not found: {}", e.getMessage());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        try {
            tmdbSearch = requestService.tmdbApiSearchRequest(query);
            // catch any response error
        } catch (HttpStatusException e) {
            QueryResult errorQuery = new QueryResult();
            errorQuery.setTheMovieDbId(-1);
            errorQuery.setTitle(e.getUrl());
            errorQuery.setDescription("Connection error: " + e.getStatusCode());
            queryResults.add(errorQuery);
            LOG.error("[ search ] Connection error: {} @ {}", e.getStatusCode(), e.getUrl());
        } catch (UnknownHostException e) {
            QueryResult errorQuery = new QueryResult();
            errorQuery.setTheMovieDbId(-1);
            errorQuery.setTitle(e.getMessage());
            errorQuery.setDescription("[ search ] Host not found");
            queryResults.add(errorQuery);
            LOG.error("[ search ] Host not found: {}", e.getMessage());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        // in case of any errors or no results return empty list
        if (!webSearchResults.isEmpty()) {
            queryResults.addAll(
                    responseParser.parseWebSearchResults(webSearchResults, filePath, mediaIdentity)
            );
        }
        if (!tmdbSearch.isEmpty()) {
            queryResults.addAll(
                    responseParser.parseTmdbApiSearchResults(tmdbSearch, filePath)
            );
        }
        lastRequest = queryResults;
        return queryResults;
    }

    @Override
    public List<QueryResult> getLatestMediaQuery() {
        return lastRequest;
    }

    /*
    * Generate search query with given phrase and media identity.
    * */
    private String generateQuery(String phrase, MediaIdentity mediaIdentity) {
        if (mediaIdentity.equals(MediaIdentity.TMDB)) {
        return networkProperties.getProperty("search_url_get") +
                networkProperties.getProperty("pre_query") +
                " " +
                phrase +
                " " +
                networkProperties.getProperty("post_query");
        }
        if (mediaIdentity.equals(MediaIdentity.IMDB)) {
            return networkProperties.getProperty("imdb_pre_query") +
                    phrase +
                    " " +
                    networkProperties.getProperty("imdb_post_query") +
                    networkProperties.getProperty("imdb_query_options");
        }
        return "";
    }

    @Override
    public MediaLink createSymLink(QueryResult queryResult, MediaIdentity mediaIdentity) {
        // naming pattern -> Film (2018) [tmdbid-65567]
        // send request to themoviedb api with given query result
        MediaData mediaData = new MediaData();
        try {
            String response = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentity);
            LOG.info("[ json ] extracting data...");
            LOG.info("query: {}", queryResult);
            if (mediaIdentity.equals(MediaIdentity.TMDB))
                mediaData = responseParser.parseDetailsRequestByTmdbId(response);
                mediaData.setTmdbId(queryResult.getTheMovieDbId());
            if (mediaIdentity.equals(MediaIdentity.IMDB)){
                mediaData = responseParser.parseDetailsRequestByExternalId(response);
                mediaData.setImdbId(queryResult.getImdbId());
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        if (mediaData.getTitle() == null || mediaData.getTitle().isEmpty()) {
            LOG.error("[ symlink ] Unable to create sym link, MediaData object is empty");
            return new MediaLink();
        }

        Path targetPath = Path.of(queryResult.getFilePath());

        Path sourcePath = createLinkPath(queryResult, mediaData, MediaIdentity.IMDB);
        /*
         * Create MediaLink object
         * */
        MediaLink mediaLink = new MediaLink();
        mediaLink.setTargetPath(targetPath.toString());
        mediaLink.setLinkPath(sourcePath.toString());
        mediaLink.setTheMovieDbId(mediaData.getTmdbId());
        mediaLink.setImdbId(mediaData.getImdbId());
        boolean success = false;
        try {
            /*
            * Create parent folder if not exist
            * */
            if (!Files.exists(sourcePath.getParent())) {
                Files.createDirectories(sourcePath.getParent());
                LOG.info("[ symlink ] creating folder...");
            }
            /*
            * Create symbolic link for media file
            * */
            Files.createSymbolicLink(sourcePath, targetPath);
            LOG.info("[ symlink ] creating symlink");
            success = true;
        } catch (FileAlreadyExistsException e) {
            LOG.error("[ symlink ] Link already exists: {}", e.getMessage());
            success = true;
        } catch (IOException | SecurityException e) {
            LOG.error(e.getMessage(), e);
        }
        if (success) {
             /*
             * add link to db
             * */
            mediaTrackerDao.addNewLink(mediaLink);
            /*
            * remove query after creating symlink
            * */
            MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(queryResult.getFilePath());
            mediaTrackerDao.removeQueryFromQueue(queryByFilePath);
            LOG.info("[ symlink ] {} => {}", mediaLink.getLinkPath(), mediaLink.getTargetPath());
        }
        return mediaLink;
    }

    /*
    * Create file path for symlink file with given query result and media data
    * */
    private Path createLinkPath(QueryResult queryResult, MediaData mediaData, MediaIdentity mediaIdentity) {
        Path linkRootFolder = props.getLinksFolder();

        // check if movie is divided into multiple parts
        int discNumber = checkForMultiDiscs(queryResult.getFilePath());
        String part = (discNumber > 0) ? "-cd" + discNumber : "";

        // check for illegal characters in title
        String title = replaceIllegalCharacters(mediaData.getTitle());

        // get year
        int year = mediaData.getYear();
        String yearFormatted = " (" + year + ")";

        // get imdb id
        String imdbId = mediaData.getImdbId();
        int tmdbId = queryResult.getTheMovieDbId();
        String idFormatted = "";
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + tmdbId + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + imdbId + "]";
        }

        // get file extension
        String extension = getExtension(queryResult.getFilePath());

        // get special identifier for movie extras
        String special = "";

        // build path names
        LOG.info("[ symlink ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        String movieName = title + part + special + "." + extension;
        return linkRootFolder.resolve(movieFolder).resolve(movieName);
    }

    @Override
    public MediaQuery moveBackToQueue(MediaLink mediaLink) {
        Path linkPath = Path.of(mediaLink.getLinkPath());
        mediaTrackerDao.removeLink(mediaLink);
        cleanerService.deleteElement(linkPath.getParent());
        MediaQuery mediaQuery = new MediaQuery();
        mediaQuery.setFilePath(mediaLink.getTargetPath());
        return mediaQuery;
    }

    @Override
    public List<MediaLink> getMediaLinks() {
        return mediaTrackerDao.getAllMediaLinks();
    }

    @Override
    public void deleteInvalidLinks() {
        cleanerService.deleteInvalidLinks(props.getLinksFolder(), mediaTrackerDao);
    }

    /*
     * Extracts file extension.
     * */
    private String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /*
     * Search for phrases within filename that indicate multi disc release.
     * If that is the case, it returns number that points to the part of the movie.
     * */
    private int checkForMultiDiscs(String filename) {
        List<String> pattern = Arrays.asList("cd[^A-Za-z]?\\d", "disc[^A-Za-z]?\\d", "part[^A-Za-z]?\\d", "chapter[^A-Za-z]?\\d", "s\\d\\de\\d\\d");
        String nameOnly = filename.substring(filename.lastIndexOf("\\") + 1, filename.lastIndexOf(".")).toLowerCase();
        for (String s : pattern) {
            Pattern p = Pattern.compile(s);
            Matcher m = p.matcher(nameOnly);
            if (m.find()) {
                String found = m.group();
                String noDigit = "\\D";
                String output = found.replaceAll(noDigit, "");
                return Integer.parseInt(output);
            }
        }
        return 0;
    }

    /*
     * Replaces all illegal characters within provided string with underscores
     * */
    private String replaceIllegalCharacters(String title) {
        String illegalNames = "[#%&{}\\<>*?/$!\"+:@`|=]+";
        Pattern p = Pattern.compile(illegalNames);
        return p.matcher(title).replaceAll("_");
    }
}

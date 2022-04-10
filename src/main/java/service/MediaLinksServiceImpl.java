package service;

import dao.MediaTrackerDao;
import model.*;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import util.CleanerService;
import util.MediaIdentity;
import util.MediaType;

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
public class MediaLinksServiceImpl extends PaginationImpl implements MediaLinksService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaLinksServiceImpl.class);
    private static final String LINKS_ROOT = "LINKS_ROOT";

    private final PropertiesService props;
    private final MediaTrackerDao mediaTrackerDao;
    private final Properties networkProperties;
    private final CleanerService cleanerService;
    private final ResponseParser responseParser;
    private final RequestService requestService;

    private List<QueryResult> lastRequest;

    public MediaLinksServiceImpl(MediaTrackerDao dao, PropertiesService propertiesService,
                                 CleanerService cleanerService) {
        super(dao);
        this.cleanerService = cleanerService;
        props = propertiesService;
        networkProperties = props.getNetworkProperties();
        mediaTrackerDao = dao;
        lastRequest = null;
//        responseParser = new ResponseParser(networkProperties);
        responseParser = ResponseParser.getResponseParser(networkProperties);
        requestService = RequestService.getRequestService(networkProperties);

    }

    @Override
    public List<MediaQuery> getMediaQueryList() {
        return mediaTrackerDao.getAllMediaQueries();
    }

    // TODO custom return object with error info
    /*
     * Executes media query search using web search engine and web api search engine.
     * Return results or empty list if nothing was found.
     * On connection error it returns query result elements with error description.
     * */
    @Override
    public List<QueryResult> executeMediaQuery(String customQuery, long mediaQueryId, MediaIdentity mediaIdentity) {
        MediaQuery mediaQuery = mediaTrackerDao.getQueryById(mediaQueryId);
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
            LOG.error("[ search_web ] Connection error: {} @ {}", e.getStatusCode(), e.getUrl());
        } catch (UnknownHostException e) {
            /*
             * catch network error and pass error code as return object
             * */
            QueryResult errorQuery = new QueryResult();
            errorQuery.setTheMovieDbId(-1);
            errorQuery.setTitle(e.getMessage());
            errorQuery.setDescription("[ search_web ] Host not found");
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
            LOG.error("[ search_api ] Connection error: {} @ {}", e.getStatusCode(), e.getUrl());
        } catch (UnknownHostException e) {
            QueryResult errorQuery = new QueryResult();
            errorQuery.setTheMovieDbId(-1);
            errorQuery.setTitle(e.getMessage());
            errorQuery.setDescription("[ search ] Host not found");
            queryResults.add(errorQuery);
            LOG.error("[ search_api ] Host not found: {}", e.getMessage());
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
//    private String generateQuery(String phrase, MediaIdentity mediaIdentity) {
//        if (mediaIdentity.equals(MediaIdentity.TMDB)) {
//            return networkProperties.getProperty("search_url_get") +
//                    networkProperties.getProperty("pre_query") +
//                    " " +
//                    phrase +
//                    " " +
//                    networkProperties.getProperty("post_query");
//        }
//        if (mediaIdentity.equals(MediaIdentity.IMDB)) {
//            return networkProperties.getProperty("imdb_pre_query") +
//                    phrase +
//                    " " +
//                    networkProperties.getProperty("imdb_post_query") +
//                    networkProperties.getProperty("imdb_query_options");
//        }
//        return "";
//    }

    // TODO change return object
    @Override
    public MediaLink createSymLink(QueryResult queryResult, MediaIdentity mediaIdentity, MediaType mediaType) {
        // naming pattern -> Film (2018) [tmdbid-65567]
        // send request to themoviedb api with given query result
        MediaData mediaData = new MediaData();
        MediaLink mediaLink = new MediaLink();
        String resultMessage;
        try {
            String response = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentity);
            LOG.info("[ json ] extracting data...");
            LOG.info("query: {}", queryResult);
            if (mediaIdentity == MediaIdentity.TMDB) {
                mediaData = responseParser.parseDetailsRequestByTmdbId(response);
                mediaData.setTmdbId(queryResult.getTheMovieDbId());
            }
            if (mediaIdentity == MediaIdentity.IMDB) {
                mediaData = responseParser.parseDetailsRequestByExternalId(response);
                mediaData.setImdbId(queryResult.getImdbId());
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            new SymLinkCreationResult(false, e.getMessage(), mediaLink);
        }

        if (mediaData.getTitle() == null || mediaData.getTitle().isEmpty()) {
            resultMessage = "Unable to create sym link, MediaData object is empty";
            LOG.error("[ symlink ] {}", resultMessage);
            new SymLinkCreationResult(false, resultMessage, mediaLink);
            return mediaLink;
        }

        mediaData.setMediaType(mediaType);

        Path targetPath = Path.of(queryResult.getFilePath());
        Path linkPath = Path.of("");
        if (mediaType == MediaType.MOVIE) {
            linkPath = createMovieLinkPath(queryResult, mediaData, MediaIdentity.IMDB);
        }
        if (mediaType == MediaType.EXTRAS) {
            linkPath = createExtrasLinkPath(queryResult, mediaData, MediaIdentity.IMDB);
        }

        mediaLink.setTargetPath(targetPath.toString());
        mediaLink.setLinkPath(linkPath.toString());
        mediaLink.setTheMovieDbId(mediaData.getTmdbId());
        mediaLink.setImdbId(mediaData.getImdbId());
//        String linkPathWithCurrentRoot = linkPath.toString().replace(LINKS_ROOT, props.getLinksFolder().toString());
        Path parentPath = linkPath.getRoot().resolve(linkPath.subpath(0, linkPath.getNameCount() - 1));
        try {
            if (!Files.exists(parentPath)) {
                Files.createDirectories(parentPath);
                LOG.info("[ symlink ] creating folder...: {}", parentPath);
            }
            Files.createSymbolicLink(linkPath, targetPath);
            LOG.info("[ symlink ] creating symlink...");
        } catch (FileAlreadyExistsException e) {
            LOG.error("[ symlink ] Link already exists: {}", e.getMessage());
            new SymLinkCreationResult(false, e.getMessage(), mediaLink);
        } catch (IOException | SecurityException e) {
            LOG.error(e.getMessage());
            new SymLinkCreationResult(false, e.getMessage(), mediaLink);
        }
        boolean newLinkCreated = mediaTrackerDao.addNewLink(mediaLink);
        if (newLinkCreated) {
            MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(queryResult.getFilePath());
            mediaTrackerDao.removeQueryFromQueue(queryByFilePath);
            LOG.info("[ symlink ] {} => {}", mediaLink.getLinkPath(), mediaLink.getTargetPath());

        } else {
            cleanerService.deleteInvalidLinks(parentPath, mediaTrackerDao);
            LOG.error("[ symlink ] Cannot add link to database");
        }
        resultMessage = "New link added";
        SymLinkCreationResult symLinkCreationResult = new SymLinkCreationResult(true, resultMessage, mediaLink);
        return mediaLink;
    }

    /*
     * Create file path for symlink file with given query result and media data
     * */
    private Path createMovieLinkPath(QueryResult queryResult, MediaData mediaData,
                                     MediaIdentity mediaIdentity) {
        Path linkRootFolder = props.getLinksFolder();

        // check if movie is divided into multiple parts
        int discNumber = checkForMultiDiscs(queryResult.getFilePath());
        String part = (discNumber > 0) ? "-cd" + discNumber : "";

        String title = replaceIllegalCharacters(mediaData.getTitle());

        int year = mediaData.getYear();
        String yearFormatted = " (" + year + ")";

        String imdbId = mediaData.getImdbId();
        int tmdbId = queryResult.getTheMovieDbId();
        String idFormatted = "";
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + tmdbId + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + imdbId + "]";
        }

        String extension = getExtension(queryResult.getFilePath());

        // get special identifier for movie extras
        String special = checkForSpecialDescriptor(queryResult.getFilePath());
        special = (special.isEmpty()) ? special : " - " + special;

        // build path names
        LOG.info("[ symlink ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        LOG.info("[ symlink ] {}", movieFolder);
        String movieName = title + part + special + "." + extension;
        LOG.info("[ symlink ] {}", movieName);
//        return Path.of(LINKS_ROOT).resolve(movieFolder).resolve(movieName);
        return linkRootFolder.resolve(movieFolder).resolve(movieName);
    }

    /*
     * Finds file name elements that indicate version of the movie or definition of video format.
     * If such element exists it's going to be extracted and returned formatted, in square brackets.
     * Otherwise, empty string is returned.
     * */
    private String checkForSpecialDescriptor(String path) {
        Path fileName = Path.of(path).getFileName();
        String illegalChars = "[#%&{}\\<>*?/\\[\\]_().$!\"+:@`|=]+";
        Pattern p = Pattern.compile(illegalChars);
        String s1 = p.matcher(fileName.toString().replaceAll("'", "")).replaceAll(" ");

        List<String> specialTypes = List.of("directors cut", "theatrical cut", "unrated", "extended", "720p", "1080p", "2160p");

        StringBuilder stringBuilder = new StringBuilder();
        for (String special : specialTypes) {
            if (s1.toLowerCase().contains(special)) {
                stringBuilder.append(" ").append(special);
            }
        }
        if (stringBuilder.toString().isEmpty()) return "";
        else return "[" + stringBuilder.toString().trim() + "]";
    }

    private Path createExtrasLinkPath(QueryResult queryResult, MediaData mediaData,
                                      MediaIdentity mediaIdentity) {
        Path linkRootFolder = props.getLinksFolder();

        String title = replaceIllegalCharacters(mediaData.getTitle());

        int year = mediaData.getYear();
        String yearFormatted = " (" + year + ")";

        String imdbId = mediaData.getImdbId();
        int tmdbId = queryResult.getTheMovieDbId();
        String idFormatted = "";
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + tmdbId + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + imdbId + "]";
        }
        Path of = Path.of(queryResult.getFilePath());
        Path fileName = of.getName(of.getNameCount() - 1);

        // build path names
        LOG.info("[ symlink ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        String extrasFolder = "extras";
        String movieName = fileName.toString();
//        return Path.of(LINKS_ROOT).resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
        return linkRootFolder.resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
    }

    @Override
    public MediaQuery moveBackToQueue(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        Path linkPath = Path.of(mediaLink.getLinkPath());
        mediaTrackerDao.removeLink(mediaLink);
        cleanerService.deleteElement(linkPath.getParent());
        MediaQuery mediaQuery = new MediaQuery();
        mediaQuery.setFilePath(mediaLink.getTargetPath());
        mediaTrackerDao.addQueryToQueue(mediaQuery);
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
     * Checks whether given path of file or directory exists
     * */
    @Override
    public boolean validatePath(Path path) {
        return path.toFile().exists();
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

    // TODO
    public void moveLinksToNewLocation(Path oldLinksFolder, Path newLinksFolder) {
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        for (MediaLink ml : allMediaLinks) {
            Path oldLinkPath = Path.of(ml.getLinkPath());
            String newLinkString = oldLinkPath.toString().replace(oldLinksFolder.toString(), newLinksFolder.toString());
            Path newLinkPath = Path.of(newLinkString);
            try {
                if (!Files.exists(newLinkPath.getParent())) {
                    Files.createDirectories(newLinkPath.getParent());
                    LOG.info("[ symlink ] creating folder...: {}", newLinkPath.getParent());
                }
                Files.createSymbolicLink(newLinkPath, Path.of(ml.getTargetPath()));
                LOG.info("[ symlink ] creating symlink... {}", newLinkPath);
                ml.setLinkPath(newLinkString);
                boolean updateResult = mediaTrackerDao.updateLink(ml);
                if (updateResult) {
                    LOG.info("[ symlink ] Update successful.");
                } else {
                    LOG.error("[ symlink ] Error");
                }
            } catch (FileAlreadyExistsException e) {
                LOG.error("[ symlink ] Link already exists: {}", e.getMessage());
                new SymLinkCreationResult(false, e.getMessage(), ml);
            } catch (IOException | SecurityException e) {
                LOG.error(e.getMessage());
                new SymLinkCreationResult(false, e.getMessage(), ml);
            }
        }
        cleanerService.deleteInvalidLinks(oldLinksFolder, mediaTrackerDao);
    }

}

package service;

import com.google.gson.JsonParseException;
import dao.MediaTrackerDao;
import model.*;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import util.CleanerService;
import util.MediaIdentity;
import util.MediaType;
import util.SearchType;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public MediaLinksServiceImpl(@Qualifier("spring") MediaTrackerDao dao, PropertiesService propertiesService,
                                 CleanerService cleanerService, MediaQueryService mediaQueryService) {
        super(dao, mediaQueryService);
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
        if (queryResult.getFilePath() == null) queryResult.setFilePath("");
        if (queryResult.getImdbId() == null) queryResult.setImdbId("");
        if (queryResult.getDescription() == null) queryResult.setDescription("");
        if (queryResult.getTitle() == null) queryResult.setTitle("");
        if (queryResult.getPoster() == null) queryResult.setPoster("");
        if (queryResult.getYear() == null) queryResult.setYear("");
        return queryResult;
    }


    @Override
    public LastRequest getLatestMediaQuery() {
        return lastRequest;
    }

    @Override
    public SymLinkCreationResult createSymLink(QueryResult queryResult, MediaIdentity mediaIdentifier, MediaType mediaType) {
        // naming pattern -> Film (2018) [tmdbid-65567]
        // send request to themoviedb api with given query result
        MediaTransferData mediaTransferData = new MediaTransferData();
        MediaLink mediaLink = new MediaLink();
        String resultMessage;
        SymLinkCreationResult symLinkCreationResult = null;
        try {
            String response = requestService.tmdbApiRequestWithSpecifiedId(queryResult, mediaIdentifier);
            LOG.info("[ json ] extracting data...");
            LOG.info("query: {}", queryResult);
            if (mediaIdentifier == MediaIdentity.TMDB) {
                mediaTransferData = responseParser.parseDetailsRequestByTmdbId(response);
                mediaTransferData.setTmdbId(queryResult.getTheMovieDbId());
            }
            if (mediaIdentifier == MediaIdentity.IMDB) {
                mediaTransferData = responseParser.parseDetailsRequestByExternalId(response);
                mediaTransferData.setImdbId(queryResult.getImdbId());
            }
        } catch (HttpStatusException e) {
            LOG.error(e.getMessage());
            String message = e.getStatusCode() + " : " + e.getMessage();
            symLinkCreationResult = new SymLinkCreationResult(false, message, mediaLink);
        } catch (IOException | JsonParseException e) {
            LOG.error(e.getMessage());
            symLinkCreationResult = new SymLinkCreationResult(false, e.getMessage(), mediaLink);
        }

        if (mediaTransferData.getTitle() == null || mediaTransferData.getTitle().isEmpty()) {
            resultMessage = "Unable to create sym link, MediaData object is empty";
            LOG.error("[ symlink ] {}", resultMessage);
            symLinkCreationResult = new SymLinkCreationResult(false, resultMessage, mediaLink);
            return symLinkCreationResult;
        }

        mediaTransferData.setMediaType(mediaType);

        Path targetPath = Path.of(queryResult.getFilePath());
        Path linkPath = Path.of("");
        if (mediaType == MediaType.MOVIE) {
            linkPath = createMovieLinkPath(queryResult, mediaTransferData, LINK_IDENTIFIER);
        }
        if (mediaType == MediaType.EXTRAS) {
            linkPath = createExtrasLinkPath(queryResult, mediaTransferData, LINK_IDENTIFIER);
        }
        mediaLink.setTargetPath(targetPath.toString());
        mediaLink.setLinkPath(linkPath.toString());
        mediaLink.setTheMovieDbId(mediaTransferData.getTmdbId());
        mediaLink.setImdbId(mediaTransferData.getImdbId());

        Path parentPath = linkPath.getRoot().resolve(linkPath.subpath(0, linkPath.getNameCount() - 1));
        try {
            if (!Files.exists(targetPath)) {
                String message = "Target path not found: " + targetPath;
                LOG.error("[ symlink ] target path not found: {}", targetPath);
                return new SymLinkCreationResult(false, message, mediaLink);
            }
            if (!Files.exists(parentPath)) {
                Files.createDirectories(parentPath);
                LOG.info("[ symlink ] creating folder...: {}", parentPath);
            }
            Files.createSymbolicLink(linkPath, targetPath);
            LOG.info("[ symlink ] creating symlink...");
        } catch (FileAlreadyExistsException e) {
            LOG.error("[ symlink ] Link already exists: {}", e.getMessage());
            symLinkCreationResult = new SymLinkCreationResult(false, e.getMessage(), mediaLink);
        } catch (IOException | SecurityException e) {
            LOG.error(e.getMessage());
            symLinkCreationResult = new SymLinkCreationResult(false, e.getMessage(), mediaLink);
        }
        try {
            mediaTrackerDao.addNewLink(mediaLink);
            mediaQueryService.removeQueryByFilePath(queryResult.getFilePath());
            resultMessage = "New link added";
            symLinkCreationResult = new SymLinkCreationResult(true, resultMessage, mediaLink);
            LOG.info("[ symlink ] {} => {}", mediaLink.getLinkPath(), mediaLink.getTargetPath());
        } catch (Exception ex) {
            cleanerService.deleteInvalidLinks(parentPath, mediaTrackerDao);
            LOG.error("[ symlink ] Cannot add link to database, reason: {}", ex.getMessage());
        }
        return symLinkCreationResult;
    }

    @Override
    public MediaIgnored ignoreMediaFile(MediaQuery mediaQuery) {
//        MediaQuery queryById = mediaTrackerDao.getQueryById(mediaQueryId);
        MediaIgnored mediaIgnored = new MediaIgnored();
        String filePath = mediaQuery.getFilePath();
        mediaIgnored.setTargetPath(filePath);
        boolean success = mediaTrackerDao.addMediaIgnored(mediaIgnored);
        LOG.info("[ media ] Adding to ignore list: {}", success);
        if (success) mediaQueryService.removeQueryFromQueue(mediaQuery);
        return mediaTrackerDao.findMediaIgnoredByTargetPath(filePath);
    }

    @Override
    public List<MediaIgnored> getMediaIgnoredList() {
        return mediaTrackerDao.getAllMediaIgnored();
    }

    /*
     * Create file path for symlink file with given query result and media data
     * */
    Path createMovieLinkPath(QueryResult queryResult, MediaTransferData mediaTransferData,
                             MediaIdentity mediaIdentity) {
        // check if movie is divided into multiple parts
        int discNumber = checkForMultiDiscs(queryResult.getFilePath());
        String part = (discNumber > 0) ? "-cd" + discNumber : "";

        String title = replaceIllegalCharacters(mediaTransferData.getTitle());

        String yearFormatted = " (" + mediaTransferData.getYear() + ")";

        String idFormatted = "";
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + queryResult.getTheMovieDbId() + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + mediaTransferData.getImdbId() + "]";
        }

        String extension = getExtension(queryResult.getFilePath());

        // get special identifier for movie extras
        String special = checkForSpecialDescriptor(queryResult.getFilePath());
        String group = getGroupName(queryResult.getFilePath());
        String specialWithGroup = (special + " " + group).trim();
        specialWithGroup = (specialWithGroup.trim().isEmpty()) ? "" : " - [" + specialWithGroup + "]";
        special = (special.isEmpty()) ? special : " - " + special;

        // build path names
        LOG.info("[ symlink ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        LOG.info("[ symlink ] folder: {}", movieFolder);
        String movieName = title + part + specialWithGroup + "." + extension;
        LOG.info("[ symlink ] file: {}", movieName);
//        return Path.of(LINKS_ROOT).resolve(movieFolder).resolve(movieName);
        return linksFolder.resolve(movieFolder).resolve(movieName);
    }

    /*
     * Extracts release group name from file name if match for defined phrase is found.
     * Otherwise returns empty string.
     * */
    String getGroupName(String path) {
        String regex = "-([a-zA-Z0-9]+)(\\.\\w+)?$";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(path);
        if (matcher.find()) {
            int i = matcher.groupCount();
            if (i > 0) return matcher.group(1);
        }
        return "";
    }

    /*
     * Finds file name elements that indicate version of the movie or definition of video format.
     * If such element exists it's going to be extracted and returned formatted, in square brackets.
     * Otherwise, empty string is returned.
     * */
    String checkForSpecialDescriptor(String path) {
        Path fileName = Path.of(path).getFileName();
        String special2 = "(?i)((?:dir|inte|thea).+cut)|(\\d{3,4}p)|(unrated|extended)|\\W(hdr)\\W";
        Pattern p2 = Pattern.compile(special2);
        Matcher matcher = p2.matcher(fileName.toString());
        SortedSet<String> sortedSet = new TreeSet<>();

        while (matcher.find()) {
            sortedSet.add(
                    matcher.group()
                            .toLowerCase()
                            .replaceAll("[.]", " ")
                            .replaceAll("'", "")
                            .trim());
        }

        StringBuilder sb = new StringBuilder();
        for (String s : sortedSet) {
            sb.append(s).append(" ");
        }
        return sb.toString().trim();
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
        Path of = Path.of(queryResult.getFilePath());
        Path fileName = of.getName(of.getNameCount() - 1);

        // build path names
        LOG.info("[ symlink ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        String extrasFolder = "extras";
        String movieName = fileName.toString();
//        return Path.of(LINKS_ROOT).resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
        return linksFolder.resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
    }

    @Override
    public MediaQuery moveBackToQueue(long mediaLinkId) {
        MediaLink mediaLink = mediaTrackerDao.getLinkById(mediaLinkId);
        Path linkPath = Path.of(mediaLink.getLinkPath());
        mediaTrackerDao.removeLink(mediaLink.getMediaId());
        cleanerService.deleteElement(linkPath.getParent());
        String targetPath = mediaLink.getTargetPath();
//        MediaQuery mediaQuery = new MediaQuery();
//        mediaQuery.setFilePath(mediaLink.getTargetPath());
//        mediaTrackerDao.addQueryToQueue(mediaQuery);
        MediaQuery mediaQuery = mediaQueryService.addQueryToQueue(targetPath);
        LOG.info("[ remove_link ] Link removed for file: {}", targetPath);
        return mediaQuery;
    }

    @Override
    @Transactional
    public MediaQuery unIgnoreMedia(long mediaIgnoreId) {
//        MediaIgnored mediaIgnoredById = mediaTrackerDao.getMediaIgnoredById(mediaIgnoreId);
        MediaIgnored mediaIgnored = mediaTrackerDao.removeMediaIgnored(mediaIgnoreId);
        String targetPath = mediaIgnored.getTargetPath();
        MediaQuery mediaQuery = mediaQueryService.addQueryToQueue(targetPath);
//        MediaQuery mediaQuery = new MediaQuery();
//        mediaQuery.setFilePath(targetPath);
//        mediaTrackerDao.addQueryToQueue(mediaQuery);
        LOG.info("[ remove_link ] Link removed for file: {}", targetPath);
        return mediaQuery;
//        return mediaTrackerDao.findQueryByFilePath(targetPath);
    }

    @Override
    public List<MediaLink> getMediaLinks() {
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        return allMediaLinks;
    }

    @Override
    public void deleteInvalidLinks() {
        cleanerService.deleteInvalidLinks(linksFolder, mediaTrackerDao);
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

    public void moveLinksToNewLocation(Path oldLinksFolder, Path newLinksFolder) {
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        for (MediaLink ml : allMediaLinks) {
            Path oldLinkPath = Path.of(ml.getLinkPath());
            String newLinkString = oldLinkPath.toString().replace(oldLinksFolder.toString(), newLinksFolder.toString());
            Path newLinkPath = Path.of(newLinkString);
            try {
                if (!Files.exists(newLinkPath.getParent())) {
                    Files.createDirectories(newLinkPath.getParent());
                    LOG.info("[ symlink ] Creating folder...: {}", newLinkPath.getParent());
                }
                Files.createSymbolicLink(newLinkPath, Path.of(ml.getTargetPath()));
                LOG.info("[ symlink ] Creating symlink... {}", newLinkPath);
                ml.setLinkPath(newLinkString);
                MediaLink mediaLink = mediaTrackerDao.updateLink(ml);
                cleanerService.clearParentFolder(oldLinkPath);
                LOG.info("[ symlink ] Link moved to a new folder: {}", mediaLink);
            } catch (FileAlreadyExistsException e) {
                LOG.error("[ symlink ] Link already exists: {}", e.getMessage());
                new SymLinkCreationResult(false, e.getMessage(), ml);
            } catch (IOException | SecurityException e) {
                LOG.error(e.getMessage());
                new SymLinkCreationResult(false, e.getMessage(), ml);
            }
        }
//        cleanerService.deleteInvalidLinks(oldLinksFolder, mediaTrackerDao);
    }


}

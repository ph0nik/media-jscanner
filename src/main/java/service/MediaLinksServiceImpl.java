package service;

import com.google.gson.*;
import dao.MediaTrackerDao;
import model.MediaData;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MediaLinksServiceImpl implements MediaLinksService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaLinksServiceImpl.class);

    private final PropertiesService props;
    private final MediaTrackerDao mediaTrackerDao;
    private final Properties networkProperties;
    private final CleanerService cleanerService;

    private List<QueryResult> lastRequest;

    public MediaLinksServiceImpl(MediaTrackerDao dao, PropertiesServiceImpl propertiesService,
                                 CleanerService cleanerService) {
        this.cleanerService = cleanerService;
        props = propertiesService;
        networkProperties = props.getNetworkProperties();
        mediaTrackerDao = dao;
        lastRequest = null;
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
    public List<QueryResult> executeMediaQuery(String customQuery, MediaQuery mediaQuery) {
        String query = (customQuery.isEmpty()) ? mediaQuery.getQuery() : customQuery;
        List<QueryResult> queryResults = new ArrayList<>();
        Path filePath = Path.of(mediaQuery.getFilePath());
        String document = "";
        String tmdbSearch = "";
        try {
            document = searchEngineRequest(query);
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
        try {
            tmdbSearch = tmdbSearchRequest(query);
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
        if (!document.isEmpty()) {
            queryResults.addAll(parseReturn(document, filePath));
        }
        if (!tmdbSearch.isEmpty()) {
            queryResults.addAll(parseMovieSearch(tmdbSearch, filePath));
        }
        lastRequest = queryResults;
        return queryResults;
    }

    @Override
    public List<QueryResult> getLatestMediaQuery() {
        return lastRequest;
    }

    /*
     * Search query executed via themoviedb api
     * */
    private String tmdbSearchRequest(String query) throws IOException {
        LOG.info("[ search ] api search query: {}", query);
        String apiRequest = new StringBuilder()
                .append(networkProperties.getProperty("tmdb_api3"))
                .append(networkProperties.getProperty("tmdb_movie_search"))
                .append(networkProperties.getProperty("tmdb_request_lang"))
                .append(networkProperties.getProperty("tmdb_movie_search_query"))
                .append(query)
                .toString();
        return Jsoup.connect(apiRequest)
                .userAgent(networkProperties.getProperty("user_agent"))
                .header("Authorization", "Bearer " + networkProperties.getProperty("api_key_v4"))
                .ignoreContentType(true)
                .timeout(3000)
                .execute()
                .body();
    }

    private List<QueryResult> parseMovieSearch(String jsonString, Path path) {
        JsonElement jsonElement = JsonParser.parseString(jsonString);
        String title = "title";
        String desc = "overview";
        String id = "id";
        List<QueryResult> queryResults = new ArrayList<>();
        if (jsonElement.isJsonObject()) {
            JsonObject asJsonObject = jsonElement.getAsJsonObject();
            JsonArray results = asJsonObject.getAsJsonArray("results");
            int size = results.size();
            for (int i = 0; i < size; i++) {
                JsonElement result = results.get(i);
                if (result.isJsonObject()) {
                    QueryResult qr = new QueryResult();
                    qr.setTheMovieDbId(result.getAsJsonObject().get(id).getAsInt());
                    qr.setTitle(result.getAsJsonObject().get(title).getAsString());
                    qr.setDescription(result.getAsJsonObject().get(desc).getAsString());
                    qr.setFilePath(path.toString());
                    queryResults.add(qr);
                }
            }
        }
        return queryResults;
    }

    //"id 2005" site:imdb.com/title OR site:themoviedb.org/movie
    private String searchEngineRequest(String query) throws IOException {
        String queryFormatted = new StringBuilder()
                .append(networkProperties.getProperty("pre_query"))
                .append(" ")
                .append(query)
                .append(" ")
                .append(networkProperties.getProperty("post_query"))
                .toString();
        LOG.info("[ search ] web search query: {}", queryFormatted);
        // POST connection - redirect fails
//        Connection.Response post = Jsoup.connect(linkerProperties.getProperty("search_url_post"))
//                .data("query", queryFormatted)
//                .userAgent(linkerProperties.getProperty("User-Agent"))
//                .followRedirects(false)
//                .timeout(3000)
//                .execute();
        // GET connection
        Connection.Response response = Jsoup.connect(this.networkProperties.getProperty("search_url_get") + queryFormatted)
                .userAgent(networkProperties.getProperty("user_agent"))
                .referrer(networkProperties.getProperty("referer"))
                .header("origin", networkProperties.getProperty("origin"))
                .ignoreHttpErrors(true) // try with ignore
                .timeout(3000)
                .execute();
        LOG.info("[ response ]: {}", response.statusCode());
        return response.body();
    }

    /*
     * Parses response html element.
     * Accepts string and path.
     * */
    private List<QueryResult> parseReturn(String document, Path filePath) {
        // collection with unique objects
        Set<QueryResult> queryResultSet = new TreeSet<>();
        Document parsedDocument = Jsoup.parse(document);
        Element linksBase = parsedDocument.getElementById("links");
        // check for nulls
        if (linksBase == null) return new ArrayList<>();
        Elements linksChildren = linksBase.children();
        long id = 0;
        for (Element el : linksChildren) {
            QueryResult qr = new QueryResult();
            Elements result__title = el.getElementsByClass("result__title");
            // extract url
            String url = result__title.select("a").attr("href");
            // get the id
            String theMovieDbId = getTheMovieDbId(url);
            if (theMovieDbId != null) {
                // create object and add to collection only if id has been found
                // set id
                qr.setId(id++);
                // set url
                qr.setUrl(url);
                // set tmdb id
                qr.setTheMovieDbId(Integer.parseInt(theMovieDbId));
                // extract result__title text
                String value = result__title.select("a[href]").text();
                qr.setTitle(value);
                // extract description text
                String result__snippet = el.getElementsByClass("result__snippet").select("a[href]").text();
                qr.setDescription(result__snippet);
                // set filepath
                qr.setFilePath(filePath.toString());
                queryResultSet.add(qr);
            }
        }
        return new ArrayList<>(queryResultSet);
    }

    /*
     * Extract theMovieDb id from given url
     * */
    private String getTheMovieDbId(String url) {
        String pattern = ".+/movie/\\d+";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(url);
        // check only first instance of match
        if (m.find()) {
            String found = m.group();
            return found.substring(found.lastIndexOf('/') + 1);
        }
        return null;
    }

    @Override
    public MediaLink createSymLink(QueryResult queryResult) {
        // naming pattern -> Film (2018) [tmdbid-65567]
        // send request to themoviedb api with given query result
        MediaData mediaData = null;
        try {
            String response = tmdbApiRequest(queryResult);
            mediaData = parseMovieDetails(response);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        if (mediaData == null) throw new NoSuchElementException();

        Path targetPath = Path.of(queryResult.getFilePath());

        Path sourcePath = createLinkPath(queryResult, mediaData, MediaIdentity.IMDB);
        /*
         * Create MediaLink object
         * */
        MediaLink mediaLink = new MediaLink();
        mediaLink.setTargetPath(targetPath.toString());
        mediaLink.setLinkPath(sourcePath.toString());
        mediaLink.setTheMovieDbId(queryResult.getTheMovieDbId());
        mediaLink.setImdbId(mediaData.getImdbId());
        boolean success = false;
        System.out.println("symlink - " +sourcePath);
        try {
            if (!Files.exists(sourcePath.getParent())) {
                Files.createDirectories(sourcePath.getParent());
                LOG.info("[ symlink ] creating folder...");
            }
            Files.createSymbolicLink(sourcePath, targetPath);
            LOG.info("[ symlink ] creating symlink");
            success = true;
        } catch (FileAlreadyExistsException e) {
            LOG.error("[ symlink ] Link already exists", e);
            success = true;
        } catch (IOException | SecurityException e) {
            LOG.error(e.getMessage(), e);
        }
        if (success) {
            // add link to db
            mediaTrackerDao.addNewLink(mediaLink);
            // remove query after creating symlink
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
        String movieFolder = new StringBuilder()
                .append(title)
                .append(yearFormatted)
                .append(idFormatted)
                .toString();
        String movieName = new StringBuilder()
                .append(title)
                .append(part)
                .append(special)
                .append(".")
                .append(extension)
                .toString();
        return linkRootFolder.resolve(movieFolder).resolve(movieName);
    }

    @Override
    public MediaQuery getBackToQueue(MediaLink mediaLink) {
        Path linkPath = Path.of(mediaLink.getLinkPath());
        mediaTrackerDao.removeLink(mediaLink);
        cleanerService.deleteElement(linkPath.getParent());
        MediaQuery mediaQuery = new MediaQuery();
        mediaQuery.setFilePath(mediaLink.getTargetPath());
        return mediaQuery;
    }

    @Override
    public List<MediaLink> getMediaLinks() {
        // TODO temp plaecement of method - run on demand
        cleanerService.deleteInvalidLinks(props.getLinksFolder(), mediaTrackerDao);
        return mediaTrackerDao.getAllMediaLinks();
    }

    /*
     * TheMovieDB API request, returns json object within html body.
     * */
    private String tmdbApiRequest(QueryResult queryResult) throws IOException {
        String apiRequest = new StringBuilder()
                .append(networkProperties.getProperty("tmdb_api3"))
                .append(networkProperties.getProperty("tmdb_movie_category"))
                .append(queryResult.getTheMovieDbId())
                .append(networkProperties.getProperty("tmdb_request_lang"))
                .toString();
        String document = Jsoup.connect(apiRequest)
                .userAgent(networkProperties.getProperty("user_agent"))
                .header("Authorization", "Bearer " + networkProperties.getProperty("api_key_v4"))
                .ignoreContentType(true)
                .timeout(3000)
                .execute()
                .body();
        return document;
    }

    /*
     * Returns Media Data object consisting of title and year elements if found, otherwise returns null.
     * Accepts json object as String.
     * */
    private MediaData parseMovieDetails(String jsonWithinHtmlBody) {
        try {
            LOG.info("[ json ] extracting data...");
            JsonElement jsonElement = JsonParser.parseString(jsonWithinHtmlBody);
            String titleElement = networkProperties.getProperty("tmdb_movietitle");
            String yearElement = networkProperties.getProperty("tmdb_movieyear");
            String imdbId = networkProperties.getProperty("tmdb_imdb");
            if (jsonElement.isJsonObject()) {
                JsonObject asJsonObject = jsonElement.getAsJsonObject();
                if (asJsonObject.has(titleElement) && asJsonObject.has(yearElement)) {
                    MediaData mediaData = new MediaData();
                    mediaData.setTitle(asJsonObject.get(titleElement).getAsString());
                    String rawDate = asJsonObject.get(yearElement).getAsString();
                    LocalDate ld = LocalDate.parse(rawDate);
                    mediaData.setYear(ld.getYear());
                    mediaData.setImdbId(asJsonObject.get(imdbId).getAsString());
                    return mediaData;
                }
            }
        } catch (JsonParseException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
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

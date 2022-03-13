package service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dao.MediaTrackerDao;
import model.MediaData;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MediaLinksServiceImpl implements MediaLinksService {

//    private static final String NETWORK_PROPERTIES_FILE = "src/main/resources/network.properties";

    private MediaTrackerDao mediaTrackerDao;
    private Properties networkProperties;
    private Properties mediaFoldersProperties;
    private SymLinkProperties props;
    private List<QueryResult> lastRequest;

    public MediaLinksServiceImpl(MediaTrackerDao dao, SymLinkProperties symLinkProperties) {
        props = symLinkProperties;
        networkProperties = symLinkProperties.getNetworkProperties();
        mediaFoldersProperties = symLinkProperties.getMediaFoldersProperties();
        mediaTrackerDao = dao;
        lastRequest = null;
    }

    @Override
    public List<MediaQuery> getMediaQueryList() {
        List<MediaQuery> mq =  mediaTrackerDao.getAllMediaQueries();
        if (mq == null) return List.of();
        return mq;
    }


    @Override
    public List<QueryResult> executeMediaQuery(String customQuery, MediaQuery mediaQuery) {
        String query = (customQuery.isEmpty()) ? mediaQuery.getQuery() : customQuery;
        String document = null;
        try {
            System.out.println("[ query ]: " + customQuery);
            document = searchEngineRequest(query);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<QueryResult> queryResults = parseReturn(document, Path.of(mediaQuery.getFilePath()));
        lastRequest = queryResults;
        return queryResults;
    }

    @Override
    public List<QueryResult> getLatestMediaQuery() {
        return lastRequest;
    }

    private String searchEngineRequest(String query) throws IOException {
        String queryFormatted = new StringBuilder()
                .append(networkProperties.getProperty("pre_query"))
                .append(" ")
                .append(query)
                .append(" ")
                .append(networkProperties.getProperty("post_query"))
                .toString();
        System.out.println("[ search ]: " + queryFormatted);
        // POST connection - redirect fails
//        Connection.Response post = Jsoup.connect(linkerProperties.getProperty("search_url_post"))
//                .data("query", queryFormatted)
//                .userAgent(linkerProperties.getProperty("User-Agent"))
//                .followRedirects(false)
//                .timeout(3000)
//                .execute();
        // GET connection
        Connection.Response response = Jsoup.connect(networkProperties.getProperty("search_url_get") + queryFormatted)
                .userAgent(networkProperties.getProperty("user_agent"))
                .timeout(3000)
                .execute();
        System.out.println("[ response ]: " + response.statusCode());
        return response.body();
    }

    /*
     * Parses response html element.
     * Accepts string and path.
     * */
    private List<QueryResult> parseReturn(String document, Path filePath) {
        Set<QueryResult> queryResultSet = new TreeSet<>();
        Document parsedDocument = Jsoup.parse(document);
        Element linksBase = parsedDocument.getElementById("links");
        // check for nulls
        if (linksBase == null) return new ArrayList<>();
        Elements linksChildren = linksBase.children();
        // collection with unique objects
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
        // Film (2018) [tmdbid-65567]
        // send request to themoviedb api with given query result
        MediaData mediaData = null;
        try {
            String response = tmdbApiRequest(queryResult);
            mediaData = parseJson(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mediaData == null) throw new NoSuchElementException();
//        Path linkRootFolder = SymLinkProperties.getMovieFolder();
        Path linkRootFolder = props.getLinksFolder();
        Path targetPath = Path.of(queryResult.getFilePath());
        // check for number of parts
        int discNumber = checkForMultiDiscs(queryResult.getFilePath());
        // check for illegal characters in title
        String title = replaceIllegalCharacters(mediaData.getTitle());
        int year = mediaData.getYear();
        String extension = getExtension(queryResult.getFilePath());
        String special = "";
        String part = (discNumber > 0) ? "-cd" + discNumber : "";
        // build path names
        System.out.println("[ symlink ] creating path names...");
        StringBuilder movieFolder = new StringBuilder()
                .append(title)
                .append(" (")
                .append(year)
                .append(") ")
                .append("[tmdbid-")
                .append(queryResult.getTheMovieDbId())
                .append("]");
        StringBuilder movieName = new StringBuilder()
                .append(title)
                .append(part)
                .append(special)
                .append(".")
                .append(extension);
        Path sourcePath = linkRootFolder
                .resolve(movieFolder.toString());
        Path sourceFile = Path.of(movieName.toString());
        // Create MediaLink object
        MediaLink mediaLink = new MediaLink();
        mediaLink.setTargetPath(targetPath.toString());
//        mediaLink.setParentPath(targetPath.getParent().toString());
        mediaLink.setLinkPath(sourcePath.resolve(sourceFile).toString());
        mediaLink.setTheMovieDbId(queryResult.getTheMovieDbId());
        boolean success = false;
        try {
            if (!Files.exists(sourcePath)) {
                Files.createDirectories(sourcePath);
                System.out.println("[ symlink ] creating folder...");
            }
            System.out.println("[ symlink ] creating symlink");
            Files.createSymbolicLink(sourcePath.resolve(sourceFile), targetPath);
            success = true;
        } catch (FileAlreadyExistsException e) {
            System.out.println("[ symlink ] Link already exists");
            success = true;
        } catch (IOException | SecurityException e) {
            System.out.println(e.getMessage());
        }
        if (success) {
            // add link to db
            mediaTrackerDao.addNewLink(mediaLink);
            // remove query after creating symlink
            MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(queryResult.getFilePath());
            mediaTrackerDao.removeQueryFromQueue(queryByFilePath);
            System.out.println("[ symlink ] " + mediaLink.getLinkPath() + " => " + mediaLink.getTargetPath());
        }
        return mediaLink;
    }

    @Override
    public List<MediaLink> getMediaLinks() {
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
     * Returns string value of title element if found, otherwise returns null.
     * Accepts json object as String.
     * */
    private MediaData parseJson(String jsonWithinHtmlBody) {
        try {
            System.out.println("[ json ] extracting data...");
            JsonElement jsonElement = JsonParser.parseString(jsonWithinHtmlBody);
            String titleElement = networkProperties.getProperty("tmdb_movietitle");
            String yearElement = networkProperties.getProperty("tmdb_movieyear");
            if (jsonElement.isJsonObject()) {
                JsonObject asJsonObject = jsonElement.getAsJsonObject();
                if (asJsonObject.has(titleElement) && asJsonObject.has(yearElement)) {
                    MediaData mediaData = new MediaData();
                    mediaData.setTitle(asJsonObject.get(titleElement).getAsString());
                    String rawDate = asJsonObject.get(yearElement).getAsString();
                    LocalDate ld = LocalDate.parse(rawDate);
                    mediaData.setYear(ld.getYear());
                    return mediaData;
                }
            }
        } catch (JsonParseException e) {
            System.out.println(e.getMessage());
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

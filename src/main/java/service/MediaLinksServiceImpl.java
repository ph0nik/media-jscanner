package service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.MediaData;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MediaLinksServiceImpl implements MediaLinksService {

    private MediaTrackerDao mediaTrackerDao;
    private Properties linkerProperties;

    public MediaLinksServiceImpl(MediaTrackerDao dao) {
        loadConnectionProperties();
        SymLinkProperties.loadSymLinkProperties();
        mediaTrackerDao = dao;
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
        return parseReturn(document, Path.of(mediaQuery.getFilePath()));
    }

    // TODO get different details from search results - regular webpage
    private String searchEngineRequest(String query) throws IOException {
        String queryFormatted = new StringBuilder()
                .append(linkerProperties.getProperty("preQuery"))
                .append(" ")
                .append(query)
                .toString();
        System.out.println("[ search ]: " + queryFormatted);
        // POST connection - redirect fails
//        Connection.Response post = Jsoup.connect(linkerProperties.getProperty("searchUrlPost"))
//                .data("query", queryFormatted)
//                .userAgent(linkerProperties.getProperty("User-Agent"))
//                .followRedirects(false)
//                .timeout(3000)
//                .execute();
        // GET connection
        Connection.Response response = Jsoup.connect(linkerProperties.getProperty("searchUrlGet") + queryFormatted)
                .userAgent(linkerProperties.getProperty("User-Agent"))
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
        Path linkRootFolder = SymLinkProperties.getMovieFolder();
        Path targetPath = Path.of(queryResult.getFilePath());
        // check for number of parts
        int discNumber = checkForMultiDiscs(queryResult.getFilePath());
        // check for illegal characters in title
        String title = characterPrison(mediaData.getTitle());
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
        MediaLink mediaLink = new MediaLink();
        mediaLink.setTargetPath(targetPath.toString());
        mediaLink.setLinkPath(sourcePath.resolve(sourceFile).toString());
        mediaLink.setTheMovieDbId(queryResult.getTheMovieDbId());
        try {
            if (!Files.exists(sourcePath)) {
                Files.createDirectories(sourcePath);
                System.out.println("[ symlink ] creating folder...");
            }
            System.out.println("[ symlink ] creating symlink");
            Files.createSymbolicLink(sourcePath.resolve(sourceFile), targetPath);
            // forgot to add dao element here, new links didn't show in db.
            mediaTrackerDao.addNewLink(mediaLink);
            // remove query after creating symlink
            MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(queryResult.getFilePath());
            mediaTrackerDao.removeQueryFromQueue(queryByFilePath);
            System.out.println("[ symlink ] " + mediaLink.getLinkPath() + " => " + mediaLink.getTargetPath());
        } catch (IOException | SecurityException e) {
            System.out.println(e.getMessage());
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
                .append(linkerProperties.getProperty("tmdb_api3"))
                .append(linkerProperties.getProperty("tmdb_movie_category"))
                .append(queryResult.getTheMovieDbId())
                .append(linkerProperties.getProperty("tmdb_request_lang"))
                .toString();
        String document = Jsoup.connect(apiRequest)
                .userAgent(linkerProperties.getProperty("User-Agent"))
                .header("Authorization", "Bearer " + linkerProperties.getProperty("api_key_v4"))
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
            String titleElement = linkerProperties.getProperty("tmdb_movietitle");
            String yearElement = linkerProperties.getProperty("tmdb_movieyear");
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
    private String characterPrison(String title) {
        String illegalNames = "[#%&{}\\<>*?/$!\"+:@`|=]+";
        Pattern p = Pattern.compile(illegalNames);
        return p.matcher(title).replaceAll("_");
    }

    /*
     * Loads properties object from configuration file.
     * */
    private void loadConnectionProperties() {
        String configPath = "src/main/resources/network.properties";
        try (InputStream is = new FileInputStream(configPath)) {
            linkerProperties = new Properties();
            linkerProperties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MediaTrackerDao mediaTrackerDao = new MediaTrackerDaoImpl();
        MediaLinksServiceImpl ml = new MediaLinksServiceImpl(mediaTrackerDao);
        String searchExample = "Army.of.Darkness.1992.Director's.Cut.Hybrid.1080p.BluRay.DTS.x264-IDE.mkv";

//        MediaQuery mq = new MediaQuery(searchExample, "folder");
//        ml.executeQuery(mq);
        Path htmlFilePath = Path.of("src/test/resources/army.html");
        Path fileFolder = Path.of("G:\\Java\\media-jscanner\\test-folder\\3\\info.txt");
        Path filePath = fileFolder.resolve(searchExample);
        System.out.println(filePath);
        String importedHtml = null;
        try {
            importedHtml = Files.readString(htmlFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // test parser
        List<QueryResult> queryResults = ml.parseReturn(importedHtml, filePath);

//        System.out.println(queryResults);

        // test api query
//        try {
//
//            String s = ml.tmdbApiRequest(queryResults.get(1));
//            ml.parseJson(s);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        // test symlink
        System.out.println(queryResults.get(1));
        MediaLink symLink = ml.createSymLink(queryResults.get(1));
        System.out.println(symLink);


    }
}

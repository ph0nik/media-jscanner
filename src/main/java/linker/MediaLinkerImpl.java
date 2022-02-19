package linker;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaLinkerImpl implements MediaLinker {

    private MediaTrackerDao mediaTrackerDao;
    private Properties linkerProperties;

    public MediaLinkerImpl() {
        loadConnectionProperties();
        SymLinkProperties.loadSymLinkProperties();
        mediaTrackerDao = new MediaTrackerDaoImpl();
    }

    @Override
    public List<MediaQuery> mediaQueryList() {
        return mediaTrackerDao.getAllMediaQueries();
    }

    @Override
    public List<QueryResult> executeQuery(MediaQuery mediaQuery) {
        String document = null;
        try {
            document = searchEngineRequest(mediaQuery.getQuery());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TODO check for null return after query
        System.out.println(document);
        return parseReturn(document, Path.of(mediaQuery.getFilePath()));
        // send jsoup request with given query
        // parse response if no errors occured
        // form QueryResult list and return
    }

    public List<QueryResult> parseReturn(String document, Path filePath) {
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
        while (m.find()) {
            String found = m.group();
            return found.substring(found.lastIndexOf('/') + 1);
        }
        return null;
    }

    private String searchEngineRequest(String query) throws IOException {
        String queryFormatted = new StringBuilder()
                .append(linkerProperties.getProperty("preQuery"))
                .append(" ")
                .append(query)
                .toString();
        // POST connection - redirect fails
//        Connection.Response post = Jsoup.connect(linkerProperties.getProperty("searchUrlPost"))
//                .data("query", queryFormatted)
//                .userAgent(linkerProperties.getProperty("User-Agent"))
//                .followRedirects(false)
//                .timeout(3000)
//                .execute();
        // GET connection
        Document document = Jsoup.connect(linkerProperties.getProperty("searchUrlGet") + queryFormatted)
                .userAgent(linkerProperties.getProperty("User-Agent"))
                .timeout(3000)
                .get();
        return document.toString();
    }

    public String tmdbApiRequest(QueryResult queryResult) throws IOException {
        String apiRequest = new StringBuilder()
                .append(linkerProperties.getProperty("tmdb_api3"))
                .append("/movie/")
                .append(queryResult.getTheMovieDbId())
                .toString();
        Document document = Jsoup.connect(apiRequest)
                .userAgent(linkerProperties.getProperty("User-Agent"))
                .header("Authorization", "Bearer " + linkerProperties.getProperty("api_key_v4"))
                .ignoreContentType(true)
//                .header("Content-Type", "application/json;charset=utf-8")
                .timeout(3000)
                .get();
        System.out.println(document);
        return null;
    }

    @Override
    public MediaLink createSymLink(QueryResult queryResult) {
        // Film (2018) [tmdbid-65567]
        // send request to themoviedb api with given query result
        // get the data
        // form path name and file name with given pattern
        // return true if all went well
        Path sourceFolder = SymLinkProperties.getMovieFolder();
        Path targetPath = Path.of(queryResult.getFilePath());
        // test data
        int discNumber = checkForMultiDiscs(queryResult.getFilePath());
        String extension = getExtension(queryResult.getFilePath());
        String special = "";
        String part = (discNumber > 0) ? "-cd" + discNumber : "";
        // build path names
        StringBuilder movieFolder = new StringBuilder("title ")
                .append("(" + "year" + ") ")
                .append("[tmdbid-" + queryResult.getTheMovieDbId() + "]");
        StringBuilder movieName = new StringBuilder("title ")
                .append("(" + "year" + ")")
                .append(part)
                .append(special)
                .append(".")
                .append(extension);
        Path sourcePath = sourceFolder
                .resolve(movieFolder.toString());
        Path sourceFile = Path.of(movieName.toString());
        try {
            if (!Files.exists(sourcePath)) {
                Files.createDirectories(sourcePath);
            }
            Files.createSymbolicLink(sourcePath.resolve(sourceFile), targetPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private int checkForMultiDiscs(String filename) {
        List<String> pattern = Arrays.asList("cd[^A-Za-z]?\\d", "disc[^A-Za-z]?\\d", "part[^A-Za-z]?\\d", "chapter[^A-Za-z]?\\d", "s\\d\\de\\d\\d");
        String nameOnly = filename.substring(filename.lastIndexOf("\\") + 1, filename.lastIndexOf(".")).toLowerCase();
        for (String s : pattern) {
            Pattern p = Pattern.compile(s);
            Matcher m = p.matcher(nameOnly);
            while (m.find()) {
                String found = m.group();
                String noDigit = "\\D";
                String output = found.replaceAll(noDigit, "");
                return Integer.parseInt(output);
            }
        }
        return 0;
    }

    private String characterPrison(String title) {

        return "";
    }


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
        MediaLinkerImpl ml = new MediaLinkerImpl();
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
        List<QueryResult> queryResults = ml.parseReturn(importedHtml, filePath);
        System.out.println(queryResults);
        try {
            ml.tmdbApiRequest(queryResults.get(1));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println(queryResults.get(1));
//        ml.createSymLink(queryResults.get(4));


    }
}

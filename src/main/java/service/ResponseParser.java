package service;

import com.google.gson.*;
import model.MediaTransferData;
import model.QueryResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.parser.FindResults;
import service.parser.MovieItem;
import service.parser.MovieResults;
import service.parser.TvItem;
import util.MediaIdentity;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class ResponseParser {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseParser.class);
    private static final String TMDB_POSTER_PREFIX = "tmdb_poster_prefix";

    private final Properties networkProperties;

    private ResponseParser(Properties networkProperties) {
        this.networkProperties = networkProperties;
    }

    static ResponseParser getResponseParser(Properties networkProperties) {
        return new ResponseParser(networkProperties);
    }

    /*
     * Parses response html element.
     * Accepts string and path.
     * */
    List<QueryResult> parseWebSearchResults(String document, Path filePath, MediaIdentity mediaIdentity) {
        // collection with unique objects
        Set<QueryResult> queryResultSet = new TreeSet<>();
        Document parsedDocument = Jsoup.parse(document);
        Element linksBase = parsedDocument.getElementById("links");
        // check for nulls
        if (linksBase == null) return new ArrayList<>();
        Elements linksChildren = linksBase.children();
        long id = 0;
        for (Element el : linksChildren) {
            QueryResult qr = new QueryResult(filePath.toString());
            Elements result__title = el.getElementsByClass("result__title");
            // extract url
            String url = result__title.select("a").attr("href");
            // get the id
            String theMovieDbId = getTheMovieDbId(url, mediaIdentity);
            // check for valid imdb id, not greater than 9 characters, some search results return invalid identifier
            if (mediaIdentity == MediaIdentity.IMDB && theMovieDbId != null && theMovieDbId.length() > 9)
                theMovieDbId = null;
            // create object and add to collection only if id has been found
            if (theMovieDbId != null) {
                qr.setId(id++);
                qr.setUrl(url);
                if (mediaIdentity.equals(MediaIdentity.TMDB))
                    qr.setTheMovieDbId(Integer.parseInt(theMovieDbId));
                if (mediaIdentity.equals(MediaIdentity.IMDB))
                    qr.setImdbId(theMovieDbId);
                // extract result__title text
                String value = result__title.select("a[href]").text();
                qr.setTitle(value);
                // extract description text
                String result__snippet = el.getElementsByClass("result__snippet").select("a[href]").text();
                qr.setDescription(result__snippet);
                // set filepath
                qr.setOriginalPath(filePath.toString());
                qr.setPoster("");
                qr.setYear("");
                queryResultSet.add(qr);
            }
        }
        return new ArrayList<>(queryResultSet);
    }

    List<QueryResult> parseTmdbApiSearchResults(String jsonString, Path path) {
        List<QueryResult> queryResults = new ArrayList<>();
        if (jsonString == null) {
            LOG.error("[ json_parser ] Input is null");
            return queryResults;
        }
        try {
            MovieResults movieResults = new Gson().fromJson(jsonString, MovieResults.class);
            queryResults = movieResults.getMovieResults().stream().map(movieItem -> {
                QueryResult qr = new QueryResult(path.toString());
                qr.setTheMovieDbId(movieItem.getId());
                qr.setTitle(movieItem.getTitle());
                qr.setDescription(movieItem.getDescription());
                String queryYear = (movieItem.getDate().length() >= 4) ? movieItem.getDate().substring(0, 4) : movieItem.getDate();
                qr.setYear(queryYear);
                String posterPath = (movieItem.getPoster() == null) ? "" : networkProperties.getProperty(TMDB_POSTER_PREFIX) + movieItem.getPoster();
                qr.setPoster(posterPath);
                return qr;
            }).collect(Collectors.toList());
        } catch (JsonSyntaxException ex) {
            LOG.error("[ json_parser ] No a json object, {}", ex.getMessage());
        }
        return queryResults;
    }

    /*
     * Extract theMovieDb id from given url
     * */
    private String getTheMovieDbId(String url, MediaIdentity mediaIdentity) {
        String pattern = "";
        if (mediaIdentity.equals(MediaIdentity.TMDB)) {
            pattern = ".+/movie/\\d+";
        }
        if (mediaIdentity.equals(MediaIdentity.IMDB)) {
            pattern = ".+title/tt\\d+";
        }
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(url);
        // check only first instance of match
        if (m.find()) {
            String found = m.group();
            return found.substring(found.lastIndexOf('/') + 1);
        } else {
            return null;
        }
    }

    QueryResult parseTmdbApiWithImdbId(String jsonString, QueryResult queryResult) {
        if (jsonString == null) {
            LOG.error("[ json_parser ] Input is null");
            return queryResult;
        }
        try {
            FindResults movieResults = new Gson().fromJson(jsonString, FindResults.class);
            if (movieResults.getMovieResults().size() != 0) {
                MovieItem movieItem = movieResults.getMovieResults().get(0);
                queryResult.setTheMovieDbId(movieItem.getId());
                queryResult.setTitle(movieItem.getTitle());
                queryResult.setDescription(movieItem.getDescription());
                String queryYear = (movieItem.getDate().length() >= 4) ? movieItem.getDate().substring(0, 4) : movieItem.getDate();
                queryResult.setYear(queryYear);
                String posterPath = (movieItem.getPoster() == null) ? "" : networkProperties.getProperty(TMDB_POSTER_PREFIX) + movieItem.getPoster();
                queryResult.setPoster(posterPath);
                return queryResult;
            }
            TvItem movieItem = movieResults.getTvResults().get(0);
            queryResult.setTheMovieDbId(movieItem.getId());
            queryResult.setTitle(movieItem.getTitle());
            queryResult.setDescription(movieItem.getDescription());
            String queryYear = (movieItem.getDate().length() >= 4) ? movieItem.getDate().substring(0, 4) : movieItem.getDate();
            queryResult.setYear(queryYear);
            String posterPath = (movieItem.getPoster() == null) ? "" : networkProperties.getProperty(TMDB_POSTER_PREFIX) + movieItem.getPoster();
            queryResult.setPoster(posterPath);
            return queryResult;
        } catch (JsonSyntaxException ex) {
            LOG.error("[ json_parser ] No a json object, {}", ex.getMessage());
        }
        return queryResult;
    }

    /*
     * Returns Media Data object consisting of title and year elements if found, otherwise returns empty object.
     * Accepts json object as String.
     * */
    MediaTransferData parseDetailsRequestByTmdbId(MediaTransferData mediaTransferData, String responseJson) throws JsonParseException {
        if (responseJson == null) {
            LOG.error("[ json_parser ] Input is null");
            return mediaTransferData;
        }
        try {
            MovieItem movieItem = new Gson().fromJson(responseJson, MovieItem.class);
            mediaTransferData.setTitle(movieItem.getTitle());
            mediaTransferData.setImdbId(movieItem.getImdbId());
            mediaTransferData.setYear(LocalDate.parse(movieItem.getDate()).getYear());
        } catch (JsonSyntaxException ex) {
            LOG.error("[ json_parser ] No a json object, {}", ex.getMessage());
        }
        return mediaTransferData;
    }

    /*
     * Parse data of response to api request for external identifier.
     * It takes json object as string and return media data object.
     * Returns empty object if parsing errors occur.
     * */
    MediaTransferData parseDetailsRequestByExternalId(MediaTransferData mediaTransferData, String responseJson) {
        System.out.println(responseJson);
        if (responseJson == null) {
            LOG.error("[ json_parser ] Input is null");
            return mediaTransferData;
        }
        try {
            FindResults movieResults = new Gson().fromJson(responseJson, FindResults.class);
            if (movieResults.getMovieResults().size() != 0) {
                MovieItem movieItem = movieResults.getMovieResults().get(0);
                mediaTransferData.setTmdbId(movieItem.getId());
                mediaTransferData.setTitle(movieItem.getTitle());
                mediaTransferData.setYear(LocalDate.parse(movieItem.getDate()).getYear());
                return mediaTransferData;
            }
            TvItem movieItem = movieResults.getTvResults().get(0);
            mediaTransferData.setTmdbId(movieItem.getId());
            mediaTransferData.setTitle(movieItem.getTitle());
            mediaTransferData.setYear(LocalDate.parse(movieItem.getDate()).getYear());
            return mediaTransferData;
        } catch (JsonSyntaxException ex) {
            LOG.error("[ json_parser ] No a json object, {}", ex.getMessage());
        }
        return mediaTransferData;
    }

}

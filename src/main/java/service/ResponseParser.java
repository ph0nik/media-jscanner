package service;

import com.google.gson.*;
import model.MediaData;
import model.QueryResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.MediaIdentity;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ResponseParser {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseParser.class);
    private static final String TMDB_MOVIE_TITLE = "tmdb_movietitle";
    private static final String TMDB_MOVIE_YEAR = "tmdb_movieyear";
    private static final String TMDB_IMDB_ID = "tmdb_imdb";
    private static final String TMDB_ID = "tmdb_id";

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
            QueryResult qr = new QueryResult();
            Elements result__title = el.getElementsByClass("result__title");
            // extract url
            String url = result__title.select("a").attr("href");
            // get the id
            String theMovieDbId = getTheMovieDbId(url, mediaIdentity);
            if (theMovieDbId != null) {
                // create object and add to collection only if id has been found
                // set id
                qr.setId(id++);
                // set url
                qr.setUrl(url);
                // set tmdb id
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
                qr.setFilePath(filePath.toString());
                queryResultSet.add(qr);
            }
        }
        return new ArrayList<>(queryResultSet);
    }

    List<QueryResult> parseTmdbApiSearchResults(String jsonString, Path path) {
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
        }
        return null;
    }

    /*
     * Returns Media Data object consisting of title and year elements if found, otherwise returns empty object.
     * Accepts json object as String.
     * */
    MediaData parseDetailsRequestByTmdbId(String responseJson) {
        MediaData mediaData = new MediaData();
        try {
            JsonElement jsonElement = JsonParser.parseString(responseJson);
            String titleElement = networkProperties.getProperty(TMDB_MOVIE_TITLE);
            String yearElement = networkProperties.getProperty(TMDB_MOVIE_YEAR);
            String imdbId = networkProperties.getProperty(TMDB_IMDB_ID);
            if (jsonElement.isJsonObject()) {
                JsonObject asJsonObject = jsonElement.getAsJsonObject();
                if (asJsonObject.has(titleElement) && asJsonObject.has(yearElement)) {
                    String title = asJsonObject.get(titleElement).getAsString();
                    String imdb = asJsonObject.get(imdbId).getAsString();
                    String rawDate = asJsonObject.get(yearElement).getAsString();
                    int year = LocalDate.parse(rawDate).getYear();
                    mediaData.setTitle(title);
                    mediaData.setYear(year);
                    mediaData.setImdbId(imdb);
                }
            }
        } catch (JsonParseException e) {
            LOG.error(e.getMessage(), e);
        }
        return mediaData;
    }

    /*
     * Parse data of response to api request for external identifier.
     * It takes json object as string and return media data object.
     * Returns empty object if parsing errors occur.
     * */
    MediaData parseDetailsRequestByExternalId(String responseJson) {
        MediaData mediaData = new MediaData();
        try {
            JsonElement jsonElement = JsonParser.parseString(responseJson);
            String titleElement = networkProperties.getProperty(TMDB_MOVIE_TITLE);
            String yearElement = networkProperties.getProperty(TMDB_MOVIE_YEAR);
            String tmdbIdElement = networkProperties.getProperty(TMDB_ID);
            LOG.warn("json elements: {} + {} ", titleElement, yearElement);
            if (jsonElement.isJsonObject()) {
                JsonObject asJsonObject = jsonElement.getAsJsonObject();
                if (asJsonObject.has("movie_results")) {
                    JsonArray asJsonArray = asJsonObject.get("movie_results").getAsJsonArray();
                    if (!asJsonArray.isEmpty()) {
                        JsonObject obj = asJsonArray.get(0).getAsJsonObject();
                        String title = obj.get(titleElement).getAsString();
                        String releaseDate = obj.get(yearElement).getAsString();
                        int year = LocalDate.parse(releaseDate).getYear();
                        int tmdbId = obj.get(tmdbIdElement).getAsInt();
                        mediaData.setTitle(title);
                        mediaData.setYear(year);
                        mediaData.setTmdbId(tmdbId);
                    }
                }
            }
        } catch (JsonParseException e) {
            LOG.error(e.getMessage(), e);
        }
        return mediaData;
    }


}

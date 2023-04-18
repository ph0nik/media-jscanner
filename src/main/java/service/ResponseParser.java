package service;

import com.google.gson.*;
import model.MediaTransferData;
import model.QueryResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import service.parser.FindResults;
import service.parser.MovieItem;
import service.parser.MovieResults;
import service.parser.TvItem;
import util.MediaIdentity;
import util.MediaType;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
class ResponseParser {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseParser.class);
    private static final String TMDB_POSTER_PREFIX = "tmdb_poster_prefix";

    private Properties networkProperties;

    private final PropertiesService propertiesService;

    public ResponseParser(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    /*
     * Parses response html element.
     * Accepts string and path.
     * */
    public List<QueryResult> parseWebSearchResults(String document, String filePath, MediaIdentity mediaIdentity) {
        // collection with unique objects
        // TODO make return list empty if nothing found
        Set<QueryResult> queryResultSet = new TreeSet<>();
        Element linksBase = Jsoup.parse(document).getElementById("links");
//        Optional<Element> links = Optional.ofNullable(Jsoup.parse(document).getElementById("links"));
        // check for nulls
        if (linksBase == null) return List.of(new QueryResult(filePath));
        Elements linksChildren = linksBase.children();
        long id = 0;
        for (Element el : linksChildren) {
            QueryResult qr = new QueryResult(filePath);
            Elements resultTitle = el.getElementsByClass("result__title");
            // extract url
            String url = resultTitle.select("a").attr("href");
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
                String value = resultTitle.select("a[href]").text();
                qr.setTitle(value);
                // extract description text
                String result__snippet = el.getElementsByClass("result__snippet").select("a[href]").text();
                qr.setDescription(result__snippet);
                // set filepath
                qr.setOriginalPath(filePath);
                qr.setPoster("");
                qr.setYear("");
                queryResultSet.add(qr);
            }
        }
        return new ArrayList<>(queryResultSet);
    }

    public List<QueryResult> multiSearchResultsParser(String jsonString, String path) {
        LOG.info("[ multi_parser ] Parsing multi search response...");
        if (jsonString == null || path == null) return List.of();
        List<QueryResult> queryResults = new LinkedList<>();
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonString);
            if (jsonElement.isJsonObject()
                    && jsonElement.getAsJsonObject().has("results")) {
                JsonArray results = jsonElement.getAsJsonObject().get("results").getAsJsonArray();
                QueryResult queryResult = null;
                for (JsonElement jo : results) {
                    String mediaType = jo.getAsJsonObject().get("media_type").getAsString();
                    if (mediaType.equals("tv")) {
                        TvItem tvItem = new Gson().fromJson(jo.toString(), TvItem.class);
                        queryResult = parseTvItem(tvItem, new QueryResult(path));
                    }
                    if (mediaType.equals("movie")) {
                        MovieItem movieItem = new Gson().fromJson(jo.toString(), MovieItem.class);
                        queryResult = parseMovieItem(movieItem, new QueryResult(path));
                    }
                    if (queryResult != null) {
                        queryResults.add(queryResult);
                    }
                }
            }
        } catch (JsonParseException ex) {
            LOG.error("[ multi_parser ] {}", ex.getMessage());
        }
        return queryResults;
    }

    public List<QueryResult> parseTmdbApiSearchResults(String jsonString, String path) {
        List<QueryResult> queryResults = List.of(new QueryResult(path));
        if (jsonString == null) {
            LOG.error("[ json_parser ] Input is null");
            return queryResults;
        }
        try {
            MovieResults movieResults = new Gson().fromJson(jsonString, MovieResults.class);
            if (movieResults == null || movieResults.getMovieResults() == null) return queryResults;
            queryResults = movieResults.getMovieResults().stream().map(movieItem -> {
                QueryResult qr = new QueryResult(path);
                qr.setTheMovieDbId(movieItem.getId());
                qr.setTitle(movieItem.getTitle());
                qr.setDescription(movieItem.getDescription());
                String queryYear = (movieItem.getDate().length() >= 4) ? movieItem.getDate().substring(0, 4) : movieItem.getDate();
                qr.setYear(queryYear);
                String posterPath = (movieItem.getPoster() == null) ? "" : propertiesService.getNetworkProperties().getProperty(TMDB_POSTER_PREFIX) + movieItem.getPoster();
                qr.setPoster(posterPath);
                return qr;
            }).collect(Collectors.toList());
            movieResults = null;
        } catch (JsonSyntaxException ex) {
            LOG.error("[ json_parser ] No a json object, {}", ex.getMessage());
        }
        return queryResults;
    }

    /*
     * Extract theMovieDb id from given url
     * */
    private String getTheMovieDbId(String url, MediaIdentity mediaIdentity) {
        String pattern = null;
        if (mediaIdentity.equals(MediaIdentity.TMDB)) {
            pattern = ".+/movie/\\d+";
        }
        if (mediaIdentity.equals(MediaIdentity.IMDB)) {
            pattern = ".+title/tt\\d+";
        }
//        Pattern p = Pattern.compile(pattern);
        Matcher m = Pattern.compile(pattern).matcher(url);
        // check only first instance of match
        if (m.find()) {
            String found = m.group();
            return found.substring(found.lastIndexOf('/') + 1);
        } else {
            return null;
        }
    }

    /*
     * Parse single MovieItem object into QueryResult
     * */
    QueryResult parseMovieItem(MovieItem movieItem, QueryResult queryResult) {
        queryResult.setTheMovieDbId(movieItem.getId());
        queryResult.setTitle(movieItem.getTitle());
        queryResult.setDescription(movieItem.getDescription());
        queryResult.setMediaType(MediaType.MOVIE);
        String queryYear = (movieItem.getDate().length() >= 4) ? movieItem.getDate().substring(0, 4) : movieItem.getDate();
        queryResult.setYear(queryYear);
        String posterPath = (movieItem.getPoster() == null) ? "" : propertiesService.getNetworkProperties().getProperty(TMDB_POSTER_PREFIX) + movieItem.getPoster();
        queryResult.setPoster(posterPath);
        return queryResult;
    }

    /*
     * Parse single TvItem object into QueryResult
     * */
    QueryResult parseTvItem(TvItem tvItem, QueryResult queryResult) {
        queryResult.setTheMovieDbId(tvItem.getId());
        queryResult.setTitle(tvItem.getTitle());
        queryResult.setMediaType(MediaType.TV);
        queryResult.setDescription(tvItem.getDescription());
        String queryYear = (tvItem.getDate().length() >= 4) ? tvItem.getDate().substring(0, 4) : tvItem.getDate();
        queryResult.setYear(queryYear);
        String posterPath = (tvItem.getPoster() == null) ? "" : propertiesService.getNetworkProperties().getProperty(TMDB_POSTER_PREFIX) + tvItem.getPoster();
        queryResult.setPoster(posterPath);
        return queryResult;
    }

    public QueryResult parseTmdbApiWithImdbId(String jsonString, QueryResult incomingQueryResult) {
        if (jsonString == null) {
            LOG.error("[ json_parser ] Input is null");
            return incomingQueryResult;
        }
        try {
            FindResults findResults = new Gson().fromJson(jsonString, FindResults.class);
            Optional<QueryResult> queryResultMovie = findResults
                    .getMovieResults()
                    .stream()
                    .findFirst()
                    .map(r -> parseMovieItem(r, incomingQueryResult));
            Optional<QueryResult> queryResultTv = findResults
                    .getTvResults()
                    .stream()
                    .findFirst()
                    .map(tv -> parseTvItem(tv, incomingQueryResult));
            return queryResultMovie.orElse(queryResultTv.orElse(incomingQueryResult));
        } catch (JsonSyntaxException ex) {
            LOG.error("[ json_parser ] Not a json object, {}", ex.getMessage());
        }
        return incomingQueryResult;
    }

    /*
     * Returns Media Data object consisting of title and year elements if found, otherwise returns empty object.
     * Accepts json object as String.
     * */
    public MediaTransferData parseDetailsRequestByTmdbId(MediaTransferData mediaTransferData, String responseJson) throws JsonParseException {
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
    * Same as above but with query result object TODO
    * */
    public QueryResult parseDetailsRequestByTmdbId(QueryResult queryResult, String responseJson) throws JsonParseException {
        if (responseJson == null) {
            LOG.error("[ json_parser ] Input is null");
            return queryResult;
        }
        try {
            MovieItem movieItem = new Gson().fromJson(responseJson, MovieItem.class);
            System.out.println(movieItem);
            queryResult.setTitle(movieItem.getTitle());
            queryResult.setImdbId(movieItem.getImdbId());
            queryResult.setYear(String.valueOf(LocalDate.parse(movieItem.getDate()).getYear()));
        } catch (JsonSyntaxException ex) {
            LOG.error("[ json_parser ] No a json object, {}", ex.getMessage());
        }
        return queryResult;
    }

    /*
     * Parse data of response to api request for external identifier.
     * It takes json object as string and return media data object.
     * Returns empty object if parsing errors occur.
     * */
    public MediaTransferData parseDetailsRequestByExternalId(MediaTransferData mediaTransferData, String responseJson) {
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

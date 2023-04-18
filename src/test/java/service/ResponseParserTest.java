package service;

import com.google.gson.Gson;
import model.QueryResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.parser.MovieItem;
import service.parser.MovieResults;
import util.MediaIdentity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseParserTest {

    static String tmdbSearchResults;
    static Path multiSearch;
    static PropertiesService propertiesService;
    static ResponseParser responseParser;

    @BeforeAll
    public static void loadTestFile() throws IOException {
        Path tmdbSearch = Paths.get("src/test/resources/json_tmdb_search_string.json");
        multiSearch = Paths.get("src/test/resources/json-multisearch.txt");
        tmdbSearchResults = Files.readString(tmdbSearch);
        propertiesService = new PropertiesServiceImpl();
        responseParser = new ResponseParser(propertiesService);
    }

    @Test
    @DisplayName("Deserialize sample results json file into given class")
    public void deserializeTmdbSearchResults() {
        int expectedId = 350758;
        String expectedTitle = "Bewitched Matches";
        MovieResults movieResults = new Gson().fromJson(tmdbSearchResults, MovieResults.class);
        assertNotNull(movieResults.getMovieResults());
        MovieItem movieItem = movieResults.getMovieResults()
                .stream()
                .filter(m -> m.getId() == expectedId)
                .findFirst()
                .orElse(new MovieItem());
        assertEquals(expectedTitle, movieItem.getTitle());
    }

    @Test
    @DisplayName("Try to parse malformed jason file")
    public void deserializeMalformedJson() {
        // malformed json object
        String wrongJson = "{'some':'strangely','formatted':'json'";
        List<QueryResult> results = responseParser.parseTmdbApiSearchResults(wrongJson, "random_path");
        assertEquals(1, results.size());
        // different type json object
        wrongJson = "{'some':'strangely','formatted':'json'}";
        results = responseParser.parseTmdbApiSearchResults(wrongJson, "random_path");
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Try to parse empty json object")
    public void parseTmdbApiSearchResultsWithEmptyJson() {
        List<QueryResult> queryResults = new ResponseParser(propertiesService).parseTmdbApiSearchResults("", "");
        assertNotNull(queryResults);
        assertFalse(queryResults.isEmpty());
    }

    @Test
    @DisplayName("Try to parse empty web search results")
    public void parseWebResultsEmpty() {
        List<QueryResult> queryResults = new ResponseParser(propertiesService).parseWebSearchResults("", "", MediaIdentity.IMDB);
        assertNotNull(queryResults);
        assertFalse(queryResults.isEmpty());
    }

    @Test
    @DisplayName("Try to parse correct multi search json object")
    public void readAndParseMultiSearchJson_correct() throws IOException {
        String s = Files.readString(multiSearch);
        List<QueryResult> results = responseParser.multiSearchResultsParser(s, "some_path");
        results.forEach(System.out::println);
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("Try to parse incorrect multi search json object")
    public void readAndParseMultiSearchJson_wrong() {
        String wrong = "{'page':1,'not_the_results':[]}";
        List<QueryResult> results = responseParser.multiSearchResultsParser(wrong, "some_path");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Try to parse malformed json object")
    public void readAndParseMultiSearchJson_invalid() {
        String invalid = "{'some':'malformed':'object']";
        List<QueryResult> results = responseParser.multiSearchResultsParser(invalid, "some_path");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Try to parse null json object")
    public void readAndParseMultiSearchJson_null() {
        List<QueryResult> results = responseParser.multiSearchResultsParser(null, "some_path");
        assertTrue(results.isEmpty());
    }

    @Test
    public void parseWebSearchResults_error() {
        String errorMessage = "If this error persists, please let us know: error-lite@duckduckgo.com";
        List<QueryResult> some_path = responseParser.parseWebSearchResults(errorMessage, "some_path", MediaIdentity.IMDB);
        System.out.println(some_path);
    }


    @Test
    @DisplayName("Parse json imdb id search results")
    void parseTmdbResultsViaQueryResult_tmdbApi() throws IOException {
        Path tmdbSearch = Paths.get("src/test/resources/tmdb_by_imdbid_json.txt");
        String stringJson = Files.readString(tmdbSearch);
        QueryResult qr = new QueryResult("/movie/file/path");
        qr.setTheMovieDbId(12345);
        qr.setTitle("Movie title");
        qr.setDescription("Movie description");
        qr.setYear("1990");
        qr.setPoster("Poster path");
        QueryResult newQueryResult = responseParser.parseTmdbApiWithImdbId(stringJson, qr);
        assertFalse(qr.getTheMovieDbId() != newQueryResult.getTheMovieDbId());
    }

}
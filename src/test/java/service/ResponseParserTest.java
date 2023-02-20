package service;

import com.google.gson.Gson;
import model.QueryResult;
import org.junit.BeforeClass;
import org.junit.Test;
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

    @BeforeClass
    public static void loadTestFile() throws IOException {
        Path tmdbSearch = Paths.get("src/test/resources/json_tmdb_search_string.json");
        multiSearch = Paths.get("src/test/resources/json-multisearch.txt");
        tmdbSearchResults = Files.readString(tmdbSearch);
        propertiesService = new PropertiesServiceImpl();
        responseParser = new ResponseParser(propertiesService);
//        tmdbSearchResults = Files.readAllLines(hd, StandardCharsets.ISO_8859_1);
    }

    @Test
    public void deserializeTmdbSearchResults() {
        int expectedId = 350758;
        String expectedTitle = "Bewitched Matches";
        MovieResults movieResults = new Gson().fromJson(tmdbSearchResults, MovieResults.class);
        assertNotNull(movieResults.getMovieResults());
        MovieItem movieItem = movieResults.getMovieResults().stream().filter(m -> m.getId() == expectedId).findFirst().get();
        assertEquals(expectedTitle, movieItem.getTitle());
    }

    @Test
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
    public void parseTmdbApiSearchResultsWithEmptyJson() {
        List<QueryResult> queryResults = new ResponseParser(propertiesService).parseTmdbApiSearchResults("", "");
        assertNotNull(queryResults);
        assertFalse(queryResults.isEmpty());
    }

    @Test
    public void parseWebResultsEmpty() {
        List<QueryResult> queryResults = new ResponseParser(propertiesService).parseWebSearchResults("", "", MediaIdentity.IMDB);
        assertNotNull(queryResults);
        assertFalse(queryResults.isEmpty());
    }

    @Test
    public void readAndParseMultiSearchJson_correct() throws IOException {
        String s = Files.readString(multiSearch);
        List<QueryResult> results = responseParser.multiSearchResultsParser(s, "some_path");
        assertFalse(results.isEmpty());
    }

    @Test
    public void readAndParseMultiSearchJson_wrong() {
        String wrong = "{'page':1,'not_the_results':[]}";
        List<QueryResult> results = responseParser.multiSearchResultsParser(wrong, "some_path");
        assertTrue(results.isEmpty());
    }

    @Test
    public void readAndParseMultiSearchJson_invalid() {
        String invalid = "{'some':'malformed':'object']";
        List<QueryResult> results = responseParser.multiSearchResultsParser(invalid, "some_path");
        assertTrue(results.isEmpty());
    }

    @Test
    public void readAndParseMultiSearchJson_null() {
        List<QueryResult> results = responseParser.multiSearchResultsParser(null, "some_path");

    }

}
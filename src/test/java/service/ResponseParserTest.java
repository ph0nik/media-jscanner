package service;

import app.config.EnvValidator;
import com.google.gson.Gson;
import model.QueryResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import service.parser.MovieItem;
import service.parser.MovieResults;
import util.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResponseParserTest {
    private String tmdbSearchResults;
    private Path multiSearch;
    private PropertiesService propertiesService;
    private ResponseParser responseParser;

    @BeforeAll
    public void loadTestFile() throws IOException, NoApiKeyException, ConfigurationException {
        Path tmdbSearch = Paths.get("src/test/resources/json_tmdb_search_string.json");
        multiSearch = Paths.get("src/test/resources/json-multisearch.txt");
        tmdbSearchResults = Files.readString(tmdbSearch);
        EnvValidator envValidator = new EnvValidator(null);
        propertiesService = new PropertiesServiceImpl(envValidator);
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
    @DisplayName("Try to parse wrong type json file")
    public void deserializeJson_wrongJson() {
        // different type json object
        String wrongJson = "{'some':'strangely','formatted':'json'}";
        List<QueryResult> results = responseParser.parseTmdbApiMovieResults(wrongJson, "random_path");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Try to parse malformed json file")
    public void deserializeJson_malformedJson() {
        // malformed json object
        String wrongJson = "{'some':'strangely'\\\\\n\n,'formatted':'json'";
        List<QueryResult> results = responseParser.parseTmdbApiMovieResults(wrongJson, "random_path");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Try to parse empty json object")
    public void deserializeJson_emptyJson() {
        List<QueryResult> queryResults = responseParser.parseTmdbApiMovieResults("", "");
        assertNotNull(queryResults);
        assertTrue(queryResults.isEmpty());
    }

    @Test
    @DisplayName("Try to parse empty web search results")
    public void parseWebResultsEmpty() {
        List<QueryResult> queryResults = responseParser.parseWebSearchResults(
                "", "", MediaType.MOVIE);
        assertNotNull(queryResults);
        assertFalse(queryResults.isEmpty());
    }

    @Test
    @DisplayName("Try to parse correct multi search json object")
    public void readAndParseMultiSearchJson_correct() throws IOException {
        String s = Files.readString(multiSearch);
        List<QueryResult> results = responseParser.parseMultiSearchResults(s, "some_path");
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("Try to parse incorrect multi search json object")
    public void readAndParseMultiSearchJson_wrong() {
        String wrong = "{'page':1,'not_the_results':[]}";
        List<QueryResult> results = responseParser.parseMultiSearchResults(wrong, "some_path");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Try to parse malformed json object")
    public void readAndParseMultiSearchJson_invalid() {
        String invalid = "{'some':'malformed':'object']";
        List<QueryResult> results = responseParser.parseMultiSearchResults(invalid, "some_path");
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Try to parse null json object")
    public void readAndParseMultiSearchJson_null() {
        List<QueryResult> results = responseParser.parseMultiSearchResults(null, "some_path");
        assertTrue(results.isEmpty());
    }

    @Test
    public void parseWebSearchResults_error() {
        String errorMessage = "If this error persists, please let us know: error-lite@duckduckgo.com";
        List<QueryResult> some_path = responseParser.parseWebSearchResults(
                errorMessage, "some_path", MediaType.MOVIE);
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
        List<QueryResult> newQueryResult = responseParser.parseTmdbApiWithImdbId(stringJson, qr);
        assertFalse(qr.getTheMovieDbId() != newQueryResult.get(0).getTheMovieDbId());
    }

    @Test
    @DisplayName("Parse web search results for tv series")
    void parseWebSearchTvResult() throws IOException {
        Path tmdbSearch = Paths.get("src/test/resources/web_search_tv_results.txt");
        String stringHtml = Files.readString(tmdbSearch);
        String filePath = "The.Man.in.the.High.Castle.S02E01.720p.WEBRip.X264-DEFLATE.mkv";
        List<QueryResult> queryResults = responseParser.parseWebSearchResults(
                stringHtml, filePath, MediaType.TV);
    }

}
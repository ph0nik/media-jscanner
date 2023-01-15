package service;

import com.google.gson.Gson;
import org.junit.BeforeClass;
import org.junit.Test;
import service.parser.MovieItem;
import service.parser.MovieResults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResponseParserTest {

    static String tmdbSearchResults;

    @BeforeClass
    public static void loadTestFile() throws IOException {
        Path tmdbSearch = Paths.get("src/test/resources/json_tmdb_search_string.json");
        tmdbSearchResults = Files.readString(tmdbSearch);
//        tmdbSearchResults = Files.readAllLines(hd, StandardCharsets.ISO_8859_1);
    }

    @Test
    public void showJson() {
        System.out.println(tmdbSearchResults);
    }

    @Test
    public void deserializeTmdbSearchResults() {
        MovieResults movieResults = new Gson().fromJson(tmdbSearchResults, MovieResults.class);
        for (MovieItem mi : movieResults.getMovieResults()) {
            System.out.println(mi);
        }

    }

}
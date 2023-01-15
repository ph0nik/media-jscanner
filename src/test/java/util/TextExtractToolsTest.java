package util;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextExtractToolsTest {

    static List<String> testFiles;

    @BeforeClass
    public static void loadTestFile() throws IOException {
        Path sd = Paths.get("src/test/resources/test_list.txt");
        Path hd = Paths.get("src/test/resources/movies_hd.txt");
        testFiles = Files.readAllLines(hd, StandardCharsets.ISO_8859_1);
    }

    @Test
    public void showGroupNames() {
        for (String p : testFiles) {
            String file = Path.of(p).getFileName().toString();
            System.out.print(file);
            System.out.print(" -> ");
            String s = TextExtractTools.replaceIllegalCharacters(file);
            System.out.println(TextExtractTools.checkForSpecialDescriptor(s));
        }
    }

    @Test
    public void extractImdbIdCorrectUrl() {
        String sampleid = "https://www.imdb.com/title/tt14138650/?ref_=fn_al_tt_1";
        String expededid = "tt14138650";
        String imdbIdFromLink = TextExtractTools.getImdbIdFromLink(sampleid);
        assertEquals(expededid, imdbIdFromLink);
    }

    @Test
    public void extractImdbIdMalformedUrl() {
        String sampleid = "https://www.imdb.com/title/?ref_=fn_al_tt_1";
        String imdbIdFromLink = TextExtractTools.getImdbIdFromLink(sampleid);
        assertEquals("", imdbIdFromLink);
        sampleid = "Some random text";
        imdbIdFromLink = TextExtractTools.getImdbIdFromLink(sampleid);
        assertEquals("", imdbIdFromLink);
    }

}
package util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextExtractToolsTest {

    static List<String> testFiles;

    @BeforeEach
    public void loadTestFile() throws IOException {
        Path sd = Paths.get("src/test/resources/test_movies_abs_paths.txt");
        Path hd = Paths.get("src/test/resources/movies_hd.txt");
        testFiles = Files.readAllLines(hd, StandardCharsets.ISO_8859_1);
    }

    @Test
    public void showGroupNames() {
        for (String p : testFiles) {
            String file = Path.of(p).getFileName().toString();
            String s = TextExtractTools.replaceIllegalCharacters(file);

//            System.out.print(file);
//            System.out.print(" -> ");
//            System.out.println(TextExtractTools.checkForSpecialDescriptor(s));
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

    @Test
    void checkCorrectNumberOfSpaces() {
        String fileName = "E:\\Filmy HD\\The.Strange.Vice.of.Mrs.Wardh.1971.RERIP.720p.BluRay.x264\\The.Strange.Vice.of.Mrs.Wardh.1971.RERIP.720p.BluRay.x264.mkv";
        String special = TextExtractTools.checkForSpecialDescriptor(fileName);
        String group = TextExtractTools.getGroupName(fileName);
        String specialWithGroup = (special + " " + group).trim();
        specialWithGroup = (specialWithGroup.trim().isEmpty()) ? "" : " - [" + specialWithGroup + "]";
        assertEquals(" - [720p x264]", specialWithGroup);
    }

    @Test
    void extractTitleFromTvElement() {
        String sample = "Masters.of.Sex.S02E06.720p.HDTV.x264-IMMERSE.mkv";
        String s = TextExtractTools.extractTitleFromTvElement(sample);
        System.out.println(s);
    }

    @Test
    void extractSeasonNumber() throws IOException {
        Path path = Paths.get("src/test/resources/seriale_lista.txt");
        String episode = "Seriale/Fear.the.Walking.Dead.S02.1080p.BluRay.x264-ROVERS/Fear.the.Walking.Dead.S02E12.1080p.BluRay.x264-ROVERS/Fear.the.Walking.Dead.S02E12.1080p.BluRay.x264-ROVERS.mkv";
        List<String> strings = Files.readAllLines(path);

        Map<String, Integer> collect = strings.stream()
                .filter((MediaFilter::validateExtension))
                .distinct()
                .collect(Collectors.toMap(
                        episodeName -> episodeName,
                        TextExtractTools::extractSeasonNumber
                ));

        assertEquals(2, collect.get(episode));

//        String sample = "Seriale/Friends.S01-S10.MULTi.1080p.HMAX.WEB-DL.DD5.1.HEVC.PACK-PSiG/Friends.S08.MULTi.1080p.HMAX.WEB-DL.DD5.1.HEVC-PSiG/Friends.S08E16.The.One.Where.Joey.Tells.Rachel.MULTi.1080p.HMAX.WEB-DL.DD5.1.HEVC-PSiG.mkv";
//        int i = TextExtractTools.extractSeasonNumber(sample);
//        System.out.println(i);
    }

}
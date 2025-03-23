package service;

import com.google.common.io.Files;
import model.DeductedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import util.TextExtractTools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AutoMatcherServiceImplTest {


    @BeforeEach
    public void initAutoMatcher() {
    }

    @Test
    @Disabled
    public void scanFilesInDirectory() {
        File testPath = new File(".\\test-folder\\movies-target\\");
        assertTrue(testPath.exists());
        List<DeductedQuery> deductedQueryList = new ArrayList<>();
        if (testPath.isDirectory()) {
            File[] files = testPath.listFiles();
            if (files != null) {
                for (File f : files) {
                    DeductedQuery deductedQuery = TextExtractTools.extractTitleAndYear(f.toString());
                    if (deductedQuery != null) deductedQueryList.add(deductedQuery);
                }
            }
        }
        assertNotEquals(0, deductedQueryList.size());
    }

    @Test
    public void extractTitleAndYearFromFileName_success() {
        String testFile1 = "A Better Tomorrow 1986 720p BluRay DD5.1 x264-DON.mkv";
        DeductedQuery deductedQuery = TextExtractTools.extractTitleAndYear(testFile1);
        System.out.println(deductedQuery);
        assertEquals("A Better Tomorrow", deductedQuery.getPhrase());
        assertEquals("1986", String.valueOf(deductedQuery.getYear()));
    }

    @Test
    public void extractTitleAndYearFromFileName_failure() {
        String testFile1 = "Computer Chess Andrew Bujalski.mp4";
        DeductedQuery deductedQuery = TextExtractTools.extractTitleAndYear(testFile1);
//        assertNull(deductedQuery);
    }

    @Test
    public void extractTitleAndYearFromEmptyFileName_failure() {
        String testFile1 = "";
        DeductedQuery deductedQuery = TextExtractTools.extractTitleAndYear(testFile1);
        assertNull(deductedQuery);
    }

    @Test
    public void searchForExtrasElementsInPath() throws IOException {
        String correctString = "Until.the.End.of.the.World.1991.720p.BluRay.x264-x0r[EXTRA-Deleted Scenes].mkv";
        File file = Paths.get("src/test/resources/max.txt").toFile();
        List<String> strings = Files.readLines(file, StandardCharsets.UTF_8);
        List<String> collect = strings.stream()
                .filter(TextExtractTools::hasExtrasInName)
                .collect(Collectors.toList());
        // check if filtered list has 2 elements
        assertEquals(1, collect.size());
        // check if filtered list contains proper element
        assertTrue(collect.contains(correctString));
    }


}
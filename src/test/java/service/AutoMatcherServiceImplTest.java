package service;

import com.google.common.io.Files;
import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.DeductedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scanner.MediaFilesScanner;
import util.CleanerService;
import util.CleanerServiceImpl;
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

    private AutoMatcherServiceImpl autoMatcherService;
    private MediaLinksService mediaLinksService;
    private PropertiesService propertiesService;
    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;
    private MediaQueryService mediaQueryService;
    private MediaFilesScanner mediaFilesScanner;

    private FileService fileService;

    @BeforeEach
    public void initAutoMatcher() {
        mediaTrackerDao = new MediaTrackerDaoImpl();

        cleanerService = new CleanerServiceImpl();
        propertiesService = new PropertiesServiceImpl();
        mediaFilesScanner = new MediaFilesScanner(mediaTrackerDao, cleanerService);
        mediaQueryService = new MediaQueryService(mediaTrackerDao, mediaFilesScanner, propertiesService);
        mediaLinksService = new MediaLinksServiceImpl();
        autoMatcherService = new AutoMatcherServiceImpl(propertiesService, mediaLinksService);
    }

    @Test
    public void scanFilesInDirectory() {
        File testPath = new File(".\\test-folder\\movies-target\\");
        assertEquals(true, testPath.exists());
        List<DeductedQuery> deductedQueryList = new ArrayList<>();
        if (testPath.isDirectory()) {
            File[] files = testPath.listFiles();
            if (files != null) {
                for (File f : files) {
                    DeductedQuery deductedQuery = autoMatcherService.extractTitleAndYear(f.toString());
                    if (deductedQuery != null) deductedQueryList.add(deductedQuery);
                }
            }
        }
        assertNotEquals(0, deductedQueryList.size());
    }

    @Test
    void extractTitleAndYearFromFileName_success() {
        String testFile1 = "A Better Tomorrow 1986 720p BluRay DD5.1 x264-DON.mkv";
        DeductedQuery deductedQuery = autoMatcherService.extractTitleAndYear(testFile1);
        assertEquals("A Better Tomorrow", deductedQuery.getPhrase());
        assertEquals("1986", deductedQuery.getYear());
    }

    @Test
    void extractTitleAndYearFromFileName_failure() {
        String testFile1 = "Computer Chess Andrew Bujalski.mp4";
        DeductedQuery deductedQuery = autoMatcherService.extractTitleAndYear(testFile1);
        assertNull(deductedQuery);
    }

    @Test
    void extractTitleAndYearFromEmptyFileName_failure() {
        String testFile1 = "";
        DeductedQuery deductedQuery = autoMatcherService.extractTitleAndYear(testFile1);
        assertNull(deductedQuery);
    }

    @Test
    void searchForExtrasElementsInPath() throws IOException {
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
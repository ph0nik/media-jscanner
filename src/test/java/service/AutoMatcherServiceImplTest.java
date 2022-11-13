package service;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.DeductedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.CleanerService;
import util.CleanerServiceImpl;
import util.TrayMenu;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class AutoMatcherServiceImplTest {

    private AutoMatcherServiceImpl autoMatcherService;
    private MediaLinksService mediaLinksService;
    private PropertiesService propertiesService;
    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;
    private MediaQueryService mediaQueryService;
    private TrayMenu trayMenu;

    @BeforeEach
    void initAutoMatcher() {
        mediaTrackerDao = new MediaTrackerDaoImpl();
        cleanerService = new CleanerServiceImpl();
        propertiesService = new PropertiesServiceImpl();
        mediaQueryService = new MediaQueryService();
        trayMenu = new TrayMenu();
        mediaLinksService = new MediaLinksServiceImpl(mediaTrackerDao, propertiesService, cleanerService, mediaQueryService);
        autoMatcherService = new AutoMatcherServiceImpl(propertiesService, mediaLinksService, trayMenu);
    }

    @Test
    void scanFilesInDirectory() {
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
    void searchForExtrasElementsInPath() {
        URL resourceAsStream = getClass().getClassLoader().getResource("max.txt");
        File testList = new File(resourceAsStream.getPath());
        try (
                Scanner sc = new Scanner(testList)){
            while (sc.hasNextLine()) {
                String temp = sc.nextLine();
                if (autoMatcherService.hasExtrasInName(temp)) {
                    System.out.println(temp);
                }
            }
        } catch (
                FileNotFoundException e) {
            e.printStackTrace();
        }
    }


}
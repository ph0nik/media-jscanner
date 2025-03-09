package service;

import app.EnvValidator;
import com.google.common.io.Files;
import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.DeductedQuery;
import model.MediaLink;
import model.MediaQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scanner.MoviesFileScanner;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import service.query.MovieQueryService;
import util.CleanerService;
import util.CleanerServiceImpl;
import util.TextExtractTools;
import websocket.config.NotificationDispatcher;

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
    private MovieQueryService movieQueryService;
    private MoviesFileScanner moviesFileScanner;
    private Pagination<MediaQuery> pagination;
    private Pagination<MediaLink> linkPagination;
    private FileService fileService;
    private RequestService requestService;
    private ResponseParser responseParser;
    private EnvValidator envValidator;
    private NotificationDispatcher notificationDispatcher;

    @BeforeEach
    public void initAutoMatcher() throws NoApiKeyException, ConfigurationException {
        mediaTrackerDao = new MediaTrackerDaoImpl();
        cleanerService = new CleanerServiceImpl();
        envValidator = new EnvValidator(null);
        propertiesService = new PropertiesServiceImpl(envValidator);
        moviesFileScanner = new MoviesFileScanner();
        pagination = new PaginationImpl<>();
        linkPagination = new PaginationImpl<>();
        requestService = new RequestService(propertiesService);
        responseParser = new ResponseParser(propertiesService);
        movieQueryService = new MovieQueryService(mediaTrackerDao, moviesFileScanner,
                propertiesService, pagination);
        mediaLinksService = new MediaLinksServiceImpl(mediaTrackerDao, propertiesService,
                cleanerService,
                new FileService(), linkPagination,
                requestService, responseParser);
        autoMatcherService = new AutoMatcherServiceImpl(requestService, responseParser,
                mediaLinksService, movieQueryService);
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
        assertEquals("A Better Tomorrow", deductedQuery.getPhrase());
        assertEquals("1986", String.valueOf(deductedQuery.getYear()));
    }

    @Test
    public void extractTitleAndYearFromFileName_failure() {
        String testFile1 = "Computer Chess Andrew Bujalski.mp4";
        DeductedQuery deductedQuery = TextExtractTools.extractTitleAndYear(testFile1);
        assertNull(deductedQuery);
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
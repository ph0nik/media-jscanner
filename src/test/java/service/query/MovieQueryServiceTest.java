package service.query;

import app.config.CacheConfig;
import app.config.EnvValidator;
import dao.MediaLinkRepository;
import dao.MediaTrackerDao;
import dao.MediaTrackerDaoJpa;
import model.MediaQuery;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import scanner.MediaFilesScanner;
import scanner.MoviesFileScanner;
import service.Pagination;
import service.PaginationImpl;
import service.PropertiesService;
import service.PropertiesServiceImpl;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import util.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
@SpringBootTest(classes = {CacheConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@EnableCaching
class MovieQueryServiceTest {

    static MediaQueryService movieQueryService;
    static MediaTrackerDao mediaTrackerDao;
    static MediaFilesScanner mediaFilesScanner;
    static PropertiesService propertiesService;
    static Pagination<MediaQuery> pagination;
    static List<MediaQuery> mediaQueryList;
    private MediaLinkRepository mediaLinkRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeAll
    void init() throws IOException, NoApiKeyException, ConfigurationException {
        mediaTrackerDao = new MediaTrackerDaoJpa(mediaLinkRepository);
        mediaFilesScanner = new MoviesFileScanner();
        EnvValidator envValidator = new EnvValidator(null);
        propertiesService = new PropertiesServiceImpl(envValidator);
        propertiesService.addTargetPathMovie(Path.of("E:\\Filmy SD\\"));
        pagination = new PaginationImpl<>();
        movieQueryService = new MovieQueryService(mediaTrackerDao, mediaFilesScanner,
                propertiesService, pagination, cacheManager);
        getFileList();
    }

    void getFileList() throws IOException {
        Path movies = Paths.get("src/test/resources/test_movies_abs_paths.txt");
        mediaQueryList = Files.readAllLines(movies, StandardCharsets.UTF_8)
                .stream()
                .map(s -> movieQueryService.createMovieQuery(Path.of(s)))
                .collect(Collectors.toList());
        movieQueryService.updateCurrentMediaQueries(mediaQueryList);
    }

    @Test
    @DisplayName("Search for existing query within query list")
    void searchForMediaQuery_existing() {
        String search = "eko eko";
        List<MediaQuery> mediaQueries = movieQueryService.searchQuery(search);
        Assertions.assertEquals(6, mediaQueries.size());
    }

    @Test
    @DisplayName("Search for non existing query within query list")
    void searchForMediaQuery_non_existing() {
        String search = "gladiator";
        List<MediaQuery> mediaQueries = movieQueryService.searchQuery(search);
        assertTrue(mediaQueries.isEmpty());
    }

    @Test
    @DisplayName("Search with null string")
    void searchForMediaQuery_malformed() {
        String search = null;
        List<MediaQuery> mediaQueries = movieQueryService.searchQuery(search);
        assertTrue(mediaQueries.isEmpty());
    }

    @Test
    @DisplayName("Get grouped queries with existing element")
    void getGroupedQueriesFromEmptyCollection_correct() {
        List<MediaQuery> currentMediaQueries = movieQueryService.getCurrentMediaQueries();
        movieQueryService.groupByParentPathBatch(currentMediaQueries);
        String search = "corridor";
        MediaQuery mediaQuery = movieQueryService.searchQuery(search)
                .stream()
                .findFirst()
                .orElse(null);
        if (mediaQuery != null) {
            List<MediaQuery> groupedQueries =
                    movieQueryService.getGroupedQueriesWithId(mediaQuery.getQueryUuid());
            assertFalse(groupedQueries.isEmpty());
        }

    }


    @Test
    @DisplayName("Get grouped queries when root map is null")
    void getGroupedQueriesFromEmptyCollection_null() {
        List<MediaQuery> currentMediaQueries = List.of();
        movieQueryService.groupByParentPathBatch(currentMediaQueries);
        String search = "Azarak";
        MediaQuery mediaQuery = movieQueryService.searchQuery(search)
                .stream()
                .findFirst()
                .orElse(null);
        System.out.println(Path.of(mediaQuery.getFilePath()));
        if (mediaQuery != null) {
            List<MediaQuery> groupedQueries =
                    movieQueryService
                            .getGroupedQueriesWithId(mediaQuery.getQueryUuid());

            Assertions.assertEquals(1, groupedQueries.size());
        }


    }

    @Test
    @DisplayName("Get grouped queries when root map is empty")
    void getGroupedQueriesFromEmptyCollection_empty() {
        movieQueryService.groupByParentPathBatch(List.of());
        String search = "down";
        MediaQuery mediaQuery = movieQueryService.searchQuery(search)
                .stream()
                .findFirst()
                .orElse(null);
        List<MediaQuery> groupedQueries = movieQueryService.getGroupedQueriesWithId(mediaQuery.getQueryUuid());
        Assertions.assertEquals(1, groupedQueries.size());
    }

    @Test
    @DisplayName("Get grouped queries based on element with non existing uuid")
    void groupFilesWithParentFolderNextToRoot_wrongUuid() {
        List<MediaQuery> currentMediaQueries = movieQueryService.getCurrentMediaQueries();
        movieQueryService.groupByParentPathBatch(currentMediaQueries);
        MediaQuery mediaQuery = new MediaQuery("", MediaType.MOVIE);
        List<MediaQuery> groupedQueries = movieQueryService.getGroupedQueriesWithId(mediaQuery.getQueryUuid());
        assertTrue(groupedQueries.isEmpty());
    }

    @Test
    @DisplayName("Get grouped queries based on element with null uuid")
    void groupFilesWithParentFolderNextToRoot_nullUuid() {
        List<MediaQuery> currentMediaQueries = movieQueryService.getCurrentMediaQueries();
        movieQueryService.groupByParentPathBatch(currentMediaQueries);
        List<MediaQuery> groupedQueries = movieQueryService.getGroupedQueriesWithId(null);
        assertTrue(groupedQueries.isEmpty());
    }

    @Test
    void matchingParentPath() {
        List<MediaQuery> mediaQuery = movieQueryService.getCurrentMediaQueries();
        String title = "magic";
        MediaQuery first = mediaQuery
                .stream()
                .filter(
                        mq -> mq.getFilePath().toLowerCase().contains(title)
                )
                .findFirst()
                .orElse(null);
        List<MediaQuery> mediaQueries = movieQueryService.extractParentPath(first, mediaQuery);
        mediaQueries.stream().forEach(System.out::println);


    }

}
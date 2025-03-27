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
import org.springframework.context.annotation.Import;
import scanner.MediaFilesScanner;
import scanner.MoviesFileScanner;
import service.*;
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

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(CacheConfig.class)
class MovieQueryServiceTest {

    static MediaQueryService mediaQueryService;
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
        pagination = new PaginationImpl<>();
        mediaQueryService = new MovieQueryService(mediaTrackerDao, mediaFilesScanner,
                propertiesService, pagination, new LiveDataService(), cacheManager);
        getFileList();
    }

    void getFileList() throws IOException {
        Path movies = Paths.get("src/test/resources/test_movies_abs_paths.txt");
        mediaQueryList = Files.readAllLines(movies, StandardCharsets.UTF_8)
                .stream()
                .map(s -> mediaQueryService.createMovieQuery(Path.of(s)))
                .collect(Collectors.toList());
        mediaQueryService.updateCurrentMediaQueries(mediaQueryList);
    }

    @Test
    @DisplayName("Search for existing query within query list")
    void searchForMediaQuery_existing() {
        String search = "eko eko";
        List<MediaQuery> mediaQueries = mediaQueryService.searchQuery(search);
        Assertions.assertEquals(6, mediaQueries.size());
    }

    @Test
    @DisplayName("Search for non existing query within query list")
    void searchForMediaQuery_non_existing() {
        String search = "gladiator";
        List<MediaQuery> mediaQueries = mediaQueryService.searchQuery(search);
        assertTrue(mediaQueries.isEmpty());
    }

    @Test
    @DisplayName("Search with null string")
    void searchForMediaQuery_malformed() {
        String search = null;
        List<MediaQuery> mediaQueries = mediaQueryService.searchQuery(search);
        assertTrue(mediaQueries.isEmpty());
    }

    @Test
    @DisplayName("Get grouped queries with existing element")
    void getGroupedQueriesFromEmptyCollection_correct() {
        List<MediaQuery> currentMediaQueries = mediaQueryService.getCurrentMediaQueries();
        mediaQueryService.groupByParentPathBatch(currentMediaQueries);
        String search = "corridor";
        MediaQuery mediaQuery = mediaQueryService.searchQuery(search)
                .stream()
                .findFirst()
                .orElse(null);
        if (mediaQuery != null) {
            List<MediaQuery> groupedQueries =
                    mediaQueryService.getGroupedQueriesWithId(mediaQuery.getQueryUuid());
            assertFalse(groupedQueries.isEmpty());
        }

    }


    @Test
    @DisplayName("Get grouped queries when root map is null")
    void getGroupedQueriesFromEmptyCollection_null() {
        List<MediaQuery> currentMediaQueries = List.of();
        mediaQueryService.groupByParentPathBatch(currentMediaQueries);
        String search = "Azarak";
        MediaQuery mediaQuery = mediaQueryService.searchQuery(search)
                .stream()
                .findFirst()
                .orElse(null);
        if (mediaQuery != null) {
            List<MediaQuery> groupedQueries =
                    mediaQueryService
                            .getGroupedQueriesWithId(mediaQuery.getQueryUuid());
            Assertions.assertEquals(1, groupedQueries.size());
        }



    }

    @Test
    @DisplayName("Get grouped queries when root map is empty")
    void getGroupedQueriesFromEmptyCollection_empty() {
        mediaQueryService.groupByParentPathBatch(List.of());
        String search = "down";
        MediaQuery mediaQuery = mediaQueryService.searchQuery(search)
                .stream()
                .findFirst()
                .orElse(null);
        List<MediaQuery> groupedQueries = mediaQueryService.getGroupedQueriesWithId(mediaQuery.getQueryUuid());
        Assertions.assertEquals(1, groupedQueries.size());
    }

    @Test
    @DisplayName("Get grouped queries based on element with non existing uuid")
    void groupFilesWithParentFolderNextToRoot_wrongUuid() {
        List<MediaQuery> currentMediaQueries = mediaQueryService.getCurrentMediaQueries();
        mediaQueryService.groupByParentPathBatch(currentMediaQueries);
        String search = "some_non_existing_phrase";
        MediaQuery mediaQuery = mediaQueryService.searchQuery(search)
                .stream()
                .findFirst()
                .orElse(new MediaQuery("", MediaType.MOVIE));
        List<MediaQuery> groupedQueries = mediaQueryService.getGroupedQueriesWithId(mediaQuery.getQueryUuid());
        assertTrue(groupedQueries.isEmpty());
    }

    @Test
    @DisplayName("Get grouped queries based on element with null uuid")
    void groupFilesWithParentFolderNextToRoot_nullUuid() {
        List<MediaQuery> currentMediaQueries = mediaQueryService.getCurrentMediaQueries();
        mediaQueryService.groupByParentPathBatch(currentMediaQueries);
        List<MediaQuery> groupedQueries = mediaQueryService.getGroupedQueriesWithId(null);
        assertTrue(groupedQueries.isEmpty());
    }

}
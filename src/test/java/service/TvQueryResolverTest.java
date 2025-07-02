package service;

import app.config.CacheConfig;
import app.config.EnvValidator;
import dao.MediaLinkRepository;
import dao.MediaTrackerDao;
import dao.MediaTrackerDaoJpa;
import model.MediaQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import scanner.MoviesFileScanner;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import service.query.MovieQueryService;
import service.query.TvQueryResolver;
import util.MediaFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@SpringBootTest(classes = CacheConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("dev")
class TvQueryResolverTest {

    private MediaTrackerDao mediaTrackerDao;
    private MoviesFileScanner moviesFileScanner;
    private PropertiesService propertiesService;
    private MovieQueryService movieQueryService;
    private Pagination<MediaQuery> pagination;
    private TvQueryResolver tvQueryResolver;
    private Path rootPath = Path.of("Seriale");
    private List<String> fileList;
    private EnvValidator envValidator;
    private MediaLinkRepository mediaLinkRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeAll
    void readAllPathsFromFile() throws IOException, NoApiKeyException, ConfigurationException {
        String listPath = "src/test/resources/seriale_lista.txt";
        fileList = Files.readAllLines(Path.of(listPath), StandardCharsets.UTF_8)
                .stream()
                .filter(MediaFilter::validateExtension)
                .collect(Collectors.toList());
        initService();
    }

    void initService() throws NoApiKeyException, ConfigurationException {
        mediaTrackerDao = new MediaTrackerDaoJpa(mediaLinkRepository);
        moviesFileScanner = new MoviesFileScanner();
        envValidator = new EnvValidator(null);
        propertiesService = new PropertiesServiceImpl(envValidator, FileSystems.getDefault());
        pagination = new PaginationImpl<>();
        movieQueryService = new MovieQueryService(mediaTrackerDao,
                moviesFileScanner, propertiesService, pagination,
                cacheManager);
        tvQueryResolver = new TvQueryResolver(movieQueryService);
    }

    @Test
    void relativizeFilePath() {
        int i = 0;
        while (i < 20) {
            Random rnd = new Random();
            Path samplePath = Path.of(fileList.get(rnd.nextInt(fileList.size())));
            Path path = tvQueryResolver.extractParentSecondToRoot(List.of(rootPath), samplePath);
            Assertions.assertTrue(samplePath.endsWith(path));
            Assertions.assertFalse(path.startsWith(rootPath));
            i++;
        }
    }

    @Test
    void groupingByParentPath() {
        Map<Path, List<Path>> pathListMap = tvQueryResolver.batchPathExtraction(List.of(rootPath), fileList);
        int allConvertedElements = 0;
        for (Path k : pathListMap.keySet()) {
            allConvertedElements += pathListMap.get(k).size();
        }
        Assertions.assertEquals(fileList.size(), allConvertedElements);
    }

    @Test
    void getFoundSeriesList() {
        List<String> foundShowsList = tvQueryResolver.getFoundShowsList(List.of(rootPath), fileList);
//        foundShowsList.stream().sorted(Comparator.naturalOrder()).forEach(System.out::println);
    }

    @Test
    void extractSeriesMainFolder() {
        List<String> foundShowsList = tvQueryResolver.extractMainSeriesFolder(List.of(rootPath), fileList);
//        foundShowsList.stream().sorted(Comparator.naturalOrder()).forEach(System.out::println);
        Random rnd = new Random();
        int i = rnd.nextInt(foundShowsList.size());
        List<Path> fileNameWithParentFolder = tvQueryResolver.getFileNameWithParentFolder(foundShowsList.get(i), fileList);
//        System.out.println(foundShowsList.get(i));
//        fileNameWithParentFolder.forEach(System.out::println);
    }

}
package service;

import dao.MediaTrackerDao;
import dao.SpringHibernateBootstrapDao;
import model.MediaQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scanner.MoviesFileScanner;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import service.query.MovieQueryService;
import service.query.TvQueryResolver;
import util.MediaFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

class TvQueryResolverTest {

    private static MediaTrackerDao mediaTrackerDao;
    private static MoviesFileScanner moviesFileScanner;
    private static PropertiesService propertiesService;
    private static MovieQueryService movieQueryService;
    private static Pagination<MediaQuery> pagination;
    private static TvQueryResolver tvQueryResolver;
    private Path rootPath = Path.of("Seriale");
    private static List<String> fileList;
    private static String testToken = "test_token";

    @BeforeAll
    static void readAllPathsFromFile() throws IOException, NoApiKeyException, ConfigurationException {
        String listPath = "src/test/resources/seriale_lista.txt";
        fileList = Files.readAllLines(Path.of(listPath), StandardCharsets.UTF_8)
                .stream()
                .filter(MediaFilter::validateExtension)
                .collect(Collectors.toList());
        initService();
    }

    static void initService() throws NoApiKeyException, ConfigurationException {
        mediaTrackerDao = new SpringHibernateBootstrapDao();
        moviesFileScanner = new MoviesFileScanner();
        propertiesService = new PropertiesServiceImpl(testToken);
        pagination = new PaginationImpl<>();
        movieQueryService = new MovieQueryService(mediaTrackerDao, moviesFileScanner, propertiesService, pagination);
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
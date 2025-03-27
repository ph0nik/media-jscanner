package service;

import app.config.CacheConfig;
import app.config.EnvValidator;
import dao.MediaLinkRepository;
import dao.MediaTrackerDao;
import dao.MediaTrackerDaoJpa;
import model.MediaQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import scanner.MediaFilesScanner;
import scanner.MoviesFileScanner;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import service.query.MovieQueryService;

@SpringBootTest
@Import(CacheConfig.class)
class MovieQueryServiceTest {

    private MovieQueryService movieQueryService;
    private MediaTrackerDao mediaTrackerDao;
    private MediaFilesScanner mediaFilesScanner;
    private PropertiesService propertiesService;
    private Pagination<MediaQuery> pagination;
    private EnvValidator envValidator;
    private MediaLinkRepository mediaLinkRepository;
    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void initService() throws NoApiKeyException, ConfigurationException {
        mediaTrackerDao = new MediaTrackerDaoJpa(mediaLinkRepository);
        mediaFilesScanner = new MoviesFileScanner();
        envValidator = new EnvValidator(null);
        propertiesService = new PropertiesServiceImpl(envValidator);
        pagination = new PaginationImpl<>();
        movieQueryService = new MovieQueryService(
                mediaTrackerDao, mediaFilesScanner,
                propertiesService, pagination,
                new LiveDataService(), cacheManager);
    }

    @Test
    void searchForMultipleWordsInSentence() {
        String sampleFolder = "Lepa.Sela.Lepo.Gore-1996-Pretty.Village.Pretty.Flame.x264-GHETTO";
        String[] words = {"sela", "village", "ghe"};
        boolean b = movieQueryService.containsAllWords(words, sampleFolder);
        Assertions.assertTrue(b);
    }

    @Test
    void checkForInstantiatedListInExtendedClass() {
        Assertions.assertNotNull(movieQueryService.getCurrentMediaQueries());
    }

}
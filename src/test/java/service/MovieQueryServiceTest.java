package service;

import app.EnvValidator;
import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.MediaQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scanner.MediaFilesScanner;
import scanner.MoviesFileScanner;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import service.query.MovieQueryService;

class MovieQueryServiceTest {

    private MovieQueryService movieQueryService;
    private MediaTrackerDao mediaTrackerDao;
    private MediaFilesScanner mediaFilesScanner;
    private PropertiesService propertiesService;
    private Pagination<MediaQuery> pagination;
    private EnvValidator envValidator;

    @BeforeEach
    void initService() throws NoApiKeyException, ConfigurationException {
        mediaTrackerDao = new MediaTrackerDaoImpl();
        mediaFilesScanner = new MoviesFileScanner();
        envValidator = new EnvValidator(null);
        propertiesService = new PropertiesServiceImpl(envValidator);
        pagination = new PaginationImpl<>();
        movieQueryService = new MovieQueryService(
                mediaTrackerDao, mediaFilesScanner, propertiesService, pagination);
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
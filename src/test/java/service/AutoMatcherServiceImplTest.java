package service;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.DeductedQuery;
import model.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.CleanerService;
import util.CleanerServiceImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class AutoMatcherServiceImplTest {

    private AutoMatcherServiceImpl autoMatcherService;
    private MediaLinksService mediaLinksService;
    private PropertiesService propertiesService;
    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;

    @BeforeEach
    void initAutoMatcher() {
        mediaTrackerDao = new MediaTrackerDaoImpl();
        cleanerService = new CleanerServiceImpl();
        propertiesService = new PropertiesServiceImpl();
        mediaLinksService = new MediaLinksServiceImpl(mediaTrackerDao, propertiesService, cleanerService);
        autoMatcherService = new AutoMatcherServiceImpl(propertiesService, mediaLinksService);
    }

    @Test
    void scanFilesInDirectory() {
        File testPath = new File("E:\\Filmy HD\\");
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
        DeductedQuery deductedQuery = deductedQueryList.get(0);
        List<QueryResult> queryResults = autoMatcherService.searchWithDeductedQuery(deductedQuery);
        queryResults.forEach(System.out::println);
    }

    @Test
    void autoMatcherTest() {
        autoMatcherService.autoMatchFiles();
    }

}
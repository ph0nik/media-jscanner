package scanner;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.MediaIgnored;
import model.MediaQuery;
import service.PropertiesService;
import service.PropertiesServiceImpl;
import util.CleanerService;
import util.CleanerServiceImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

class MediaFilesScannerTest {

    private PropertiesService propertiesService;
    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;
    private MediaFilesScanner mediaFilesScanner;

    void initService() {
        propertiesService = new PropertiesServiceImpl();
        mediaTrackerDao = new MediaTrackerDaoImpl();
        cleanerService = new CleanerServiceImpl();
        mediaFilesScanner = new MediaFilesScanner(mediaTrackerDao, cleanerService);
    }

    void watch() {
        String alucarda = "G:\\Java\\media-jscanner\\test-folder\\movies-incoming\\Arcana.1972.DVDRIP.DivX-CG.avi";
        MediaIgnored mi = new MediaIgnored();
        mi.setTargetPath(alucarda);
        mi.setMediaId(0);
        mediaTrackerDao.addMediaIgnored(mi);

        Path path = Path.of("G:\\Java\\media-jscanner\\test-folder\\movies-incoming\\");
        List<Path> paths = List.of(path);
        List<MediaQuery> mediaQueries = List.of(new MediaQuery(alucarda));
        try {
            mediaQueries = mediaFilesScanner.scanMediaFolders(paths);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        List<MediaQuery> allMediaQueries = mediaTrackerDao.getAllMediaQueries();
//        allMediaQueries.forEach(System.out::println);
        mediaQueries.forEach(System.out::println);
    }

}
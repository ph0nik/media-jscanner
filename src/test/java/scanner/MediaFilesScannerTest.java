//package scanner;
//
//import dao.MediaTrackerDao;
//import dao.MediaTrackerDaoImpl;
//import model.MediaQuery;
//import service.PropertiesService;
//import service.PropertiesServiceImpl;
//import util.CleanerService;
//import util.CleanerServiceImpl;
//
//import java.nio.file.Path;
//import java.util.List;
//
//class MediaFilesScannerTest {
//
//    private PropertiesService propertiesService;
//    private MediaTrackerDao mediaTrackerDao;
//    private CleanerService cleanerService;
//    private MediaFilesScanner mediaFilesScanner;
//
//    public void initService() {
//        propertiesService = new PropertiesServiceImpl();
//        mediaTrackerDao = new MediaTrackerDaoImpl();
//        cleanerService = new CleanerServiceImpl();
//        mediaFilesScanner = new MediaFilesScanner(mediaTrackerDao, cleanerService);
//    }
//
//    public void watch() {
//        String alucarda = "G:\\Java\\media-jscanner\\test-folder\\movies-incoming\\Arcana.1972.DVDRIP.DivX-CG.avi";
//
//
//        Path path = Path.of("G:\\Java\\media-jscanner\\test-folder\\movies-incoming\\");
//        List<Path> paths = List.of(path);
//        List<MediaQuery> mediaQueries = List.of(new MediaQuery(alucarda));
//        mediaFilesScanner.scanMediaFolders(paths, mediaTrackerDao.getAllMediaLinks());
//
////        List<MediaQuery> allMediaQueries = mediaTrackerDao.getAllMediaQueries();
////        allMediaQueries.forEach(System.out::println);
//        mediaQueries.forEach(System.out::println);
//    }
//
//}
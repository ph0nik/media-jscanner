//package runner;
//
//import dao.MediaTrackerDao;
//import dao.MediaTrackerDaoImpl;
//import service.MediaLinksService;
//import service.MediaLinksServiceImpl;
//import service.MediaQueryService;
//import service.PropertiesServiceImpl;
//import ui.MainMenu;
//import util.CleanerService;
//import util.CleanerServiceImpl;
//
//public class MenuRunner implements Runnable {
//
//    @Override
//    public void run() {
//        MediaTrackerDao dao = new MediaTrackerDaoImpl("jscanner-sqlite");
//        PropertiesServiceImpl propertiesService = new PropertiesServiceImpl();
//        CleanerService cs = new CleanerServiceImpl();
//        MediaQueryService mediaQueryService = new MediaQueryService(dao, cs);
//        MediaLinksService mls = new MediaLinksServiceImpl(dao, propertiesService, cs, mediaQueryService);
//        MainMenu menu = new MainMenu(mls);
//        menu.getMainMenu();
//
//
////        trackerThread.interrupt();
//    }
//
//}

package runner;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import service.MediaLinksService;
import service.MediaLinksServiceImpl;
import ui.MainMenu;

public class MenuRunner implements Runnable {

    @Override
    public void run() {
        MediaTrackerDao dao = new MediaTrackerDaoImpl();
        MediaLinksService mls = new MediaLinksServiceImpl(dao);
        MainMenu menu = new MainMenu(mls);
        menu.getMainMenu();


//        trackerThread.interrupt();
    }

}

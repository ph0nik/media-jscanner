package runner;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import service.MediaLinksService;
import service.MediaLinksServiceImpl;
import service.SymLinkProperties;
import ui.MainMenu;

public class MenuRunner implements Runnable {

    @Override
    public void run() {
        MediaTrackerDao dao = new MediaTrackerDaoImpl();
        SymLinkProperties symLinkProperties = new SymLinkProperties();
        MediaLinksService mls = new MediaLinksServiceImpl(dao, symLinkProperties);
        MainMenu menu = new MainMenu(mls);
        menu.getMainMenu();


//        trackerThread.interrupt();
    }

}

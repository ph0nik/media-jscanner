package runner;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import ui.MainMenu;

public class MenuRunner implements Runnable {

    private Thread trackerThread;

    public MenuRunner(Thread other) {
        trackerThread = other;
    }

    @Override
    public void run() {
        MediaTrackerDao dao = new MediaTrackerDaoImpl();
        MainMenu menu = new MainMenu(dao);
        menu.getMainMenu();
        trackerThread.interrupt();

    }

}

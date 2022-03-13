package runner;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import service.MediaTrackerService;
import service.SymLinkProperties;
import util.CleanerService;
import util.CleanerServiceImpl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;

public class TrackerRunner implements Runnable {

    public TrackerRunner(){}

    public TrackerRunner(String[] rootFoldersList) {
//        if (rootFoldersList.length == 0)
//            System.out.println("You need to provide at least one path");
//        else {
//            getRootFolders(rootFoldersList);
//        }
    }

    @Override
    public void run() {
        MediaTrackerDao dao = new MediaTrackerDaoImpl();
        CleanerService cs = new CleanerServiceImpl();
        SymLinkProperties props = new SymLinkProperties();

        List<Path> targetFolderList = props.getTargetFolderList();

        MediaTrackerService mediaTrackerService = new MediaTrackerService(dao, cs);
        WatchService watchService;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            mediaTrackerService.watch(watchService, targetFolderList);
        } catch (IOException | InterruptedException e) {
            System.out.println("[ tracker ] closing...");
//            e.printStackTrace();
        }
    }

//    private void getRootFolders(String[] folders) {
//        rootFolder = getFoldersList(folders);
//    }
}

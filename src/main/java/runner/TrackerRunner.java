package runner;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import service.MediaTrackerService;
import util.CleanerService;
import util.CleanerServiceImpl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.LinkedList;
import java.util.List;

public class TrackerRunner implements Runnable {

    private static List<Path> rootFolder;

    public TrackerRunner(String[] rootFoldersList) {
        getRootFolders(rootFoldersList);
    }

    @Override
    public void run() {
        MediaTrackerDao dao = new MediaTrackerDaoImpl();
        CleanerService cs = new CleanerServiceImpl();
        MediaTrackerService mediaTrackerService = new MediaTrackerService(dao, cs);
        List<Path> mediaFolder = rootFolder;
        WatchService watchService;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            mediaTrackerService.watch(watchService, mediaFolder);
        } catch (IOException | InterruptedException e) {
            System.out.println("[ tracker ] closing...");
//            e.printStackTrace();
        }
    }

    private void getRootFolders(String[] folders) {
        rootFolder = getFoldersList(folders);
    }

    private List<Path> getFoldersList(String[] list) {
        List<Path> paths = new LinkedList<>();
        for (String s : list) {
            paths.add(Path.of(s));
        }
        return paths;
    }
}

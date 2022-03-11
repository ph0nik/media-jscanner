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
import java.util.LinkedList;
import java.util.List;

public class TrackerRunner implements Runnable {

    private List<Path> rootFolder;

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

        /*
         * Getting file path from properties file
         * */
//        String targetFolderMovie = props.getMediaFoldersProperties().getProperty("targetFolderMovie");
        List<Path> targetFolderList = props.getTargetFolderList();

//        List<Path> targetFolderList = List.of(Path.of(targetFolderMovie));

        MediaTrackerService mediaTrackerService = new MediaTrackerService(dao, cs);
//        List<Path> mediaFolder = rootFolder;
        WatchService watchService;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            mediaTrackerService.watch(watchService, targetFolderList);
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

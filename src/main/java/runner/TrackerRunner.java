package runner;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import service.MediaTrackerService;

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
        List<Path> mediaFolder = rootFolder;
        WatchService watchService = null;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            MediaTrackerService.watch(watchService, mediaFolder, dao);
        } catch (IOException | InterruptedException e) {
            System.out.println("[ tracker ] closing...");
//            e.printStackTrace();
        }
    }

    private void getRootFolders(String[] folders) {
//        String rootFoldersFilename = "src/main/resources/root.properties";
//        try {
//            FileReader reader = new FileReader(rootFoldersFilename);
//            Properties properties = new Properties();
//            properties.load(reader);
//            String[] watched = properties.getProperty("watched").split(",");
//            rootFolder = getFoldersList(watched);
//        } catch (FileNotFoundException e) {
//            System.out.println("[ root_folder_config ] File not found: " + rootFoldersFilename);
//            e.printStackTrace();
//        } catch (IOException ioException) {
//            ioException.printStackTrace();
//        }
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

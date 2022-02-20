package tracker;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.MediaLink;
import model.MediaQuery;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;
import static util.MediaFilter.validateExtension;

public class MediaTracker {

    // pairing keys with paths
    private static final Map<WatchKey, Path> watchKeyToPathMap = new HashMap<>();

    private static void registerTree(WatchService watchService, Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            // recursively adding subdirectories to watch service
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                watchKeyToPathMap.put(key, dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void watch(WatchService watchService, List<Path> paths, MediaTrackerDao dao) throws IOException, InterruptedException {
        for (Path path : paths) {
            registerTree(watchService, path);
        }
        // after starting watchservice scan all files within watched folders
        // and compare to existing ones
        initialScan(dao);

        while (true) {
            WatchKey watchKey = watchService.take();

            for (WatchEvent<?> event : watchKey.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path eventPath = (Path) event.context(); // event type
                Path directory = watchKeyToPathMap.get(watchKey); // directory for specified event key
                Path child = directory.resolve(eventPath);

                String tempFileName = eventPath.toString();
                boolean validExtension = validateExtension(tempFileName);
                String filePath = Path.of(directory.toString(), tempFileName).toString();

                // if newly created object is directory add it to watchlist
                if (kind == ENTRY_CREATE && Files.isDirectory(child)) {
                    registerTree(watchService, child);
                }

                // if source file is deleted check if there's matching
                // symlink, remove it with db element
                if (kind == ENTRY_DELETE && validExtension) {
                    System.out.println(kind.toString());
                    removeQuery(dao, filePath);
                    removeLink(dao, filePath);
                }

                // if newly created object is file
                if (kind == ENTRY_CREATE && validExtension) {
                    System.out.println("Type " + kind.toString());
                    addNewQuery(dao, tempFileName, filePath);

                }
            }
            boolean valid = watchKey.reset();
            if (!valid) {
                watchKeyToPathMap.remove(watchKey);
                if (watchKeyToPathMap.isEmpty()) {
                    break;
                }
            }
        }
    }

    /*
    * Crosschecks initially watched folders against link database,
    * adds all untracked elements to the queue
    * */
    private static void initialScan(MediaTrackerDao dao) {
        for (WatchKey watchKey : watchKeyToPathMap.keySet()) {
            Path path = watchKeyToPathMap.get(watchKey);
            String[] list = watchKeyToPathMap.get(watchKey).toFile().list();
            List<MediaLink> allMediaLinks = dao.getAllMediaLinks();
            if (list != null) {
                for (String singleFile : list) {
                    if (validateExtension(singleFile)) {
                    String filePath = Path.of(path.toString(), singleFile).toString();
                        if (!findMatchingLink(allMediaLinks, filePath)) {
                            addNewQuery(dao, singleFile, path.toString());
                        }
                    }
                }
            }
        }
    }


    private static boolean findMatchingLink(List<MediaLink> allMediaLinks, String filePath) {
        if (allMediaLinks != null) {
            for (MediaLink mediaLink : allMediaLinks) {
                if (mediaLink.getSourcePath().equals(filePath)) return true;
            }
        }
        return false;
    }

    /*
    * Adds new query to queue
    * */
    private static void addNewQuery(MediaTrackerDao dao, String tempFileName, String filePath) {
        MediaQuery query = new MediaQuery(tempFileName, filePath);
        dao.addQueryToQueue(query);
        System.out.println("Query added to db.");
        System.out.println("filePath : " + filePath);
        System.out.println("fileName : " + tempFileName);
    }

    /*
    *  Removes existing link
    * */
    private static void removeLink(MediaTrackerDao dao, String filePath) {
        MediaLink mediaLinkByName = dao.findMediaLinkByFilePath(filePath);
        if (mediaLinkByName != null) {
            System.out.println("Found matching link");
            dao.removeLink(mediaLinkByName);
            System.out.println("Link deleted");
        }
    }

    /*
    * Removes existing query
    * */
    private static void removeQuery(MediaTrackerDao dao, String filePath) {
        MediaQuery queryByName = dao.findQueryByFilePath(filePath);
        if (queryByName != null) {
            System.out.println("Found matching query");
            dao.removeQueryFromQueue(queryByName);
            System.out.println("Query deleted");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        MediaTrackerDao dao = new MediaTrackerDaoImpl();
        String path = "./test-folder/";
        List<Path> mediaFolder = List.of(Paths.get(path));
        WatchService watchService = FileSystems.getDefault().newWatchService();
        watch(watchService, mediaFolder, dao);



    }
}

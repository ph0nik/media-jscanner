package service;

import dao.MediaTrackerDao;
import model.MediaLink;
import model.MediaQuery;
import util.CleanerService;
import util.MediaFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;
import static util.MediaFilter.validateExtension;

public class MediaTrackerService {

    //TODO create object pairing watchkey with path
    //
    // pairing keys with paths
    private final Map<WatchKey, Path> watchKeyToPathMap = new HashMap<>();
    private final Map<Path, WatchKey> pathToWatchKeyMap = new HashMap<>();
    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;

    public MediaTrackerService(MediaTrackerDao dao, CleanerService cs) {
        mediaTrackerDao = dao;
        cleanerService = cs;
    }

    private void registerTree(WatchService watchService, Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            // recursively adding subdirectories to watch service
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                watchKeyToPathMap.put(key, dir);
                pathToWatchKeyMap.put(dir, key);
                File[] files = dir.toFile().listFiles();
                /*
                * Scan every newly added folder for files with matching extensions
                * and add them to queue
                * */
                if (files != null) {
                    for (File f : files) {
                        if (MediaFilter.validateExtension(f.toString())) {
                            String s = f.toString();
                            validateAndAdd(s);
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }


    // changed from private to public
    public void watch(WatchService watchService, List<Path> paths) throws IOException, InterruptedException {
//        mediaTrackerDao = dao;
        for (Path path : paths) {
            registerTree(watchService, path);
        }
        // after starting watchservice scan all files within watched folders
        // and compare to existing ones
//        initialScan();

        while (true) {
            WatchKey watchKey = watchService.take();

            for (WatchEvent<?> event : watchKey.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path eventPath = (Path) event.context(); // event type
                Path directory = watchKeyToPathMap.get(watchKey);// get directory for specified event key
                Path child = directory.resolve(eventPath);
                WatchKey eventPathKey = pathToWatchKeyMap.get(child); // get key for event element

                String tempFileName = eventPath.toString();
                boolean validExtension = validateExtension(tempFileName);
                String filePath = Path.of(directory.toString(), tempFileName).toString();

                // if newly created object is directory add it to watchlist
                if (kind == ENTRY_CREATE && Files.isDirectory(child)) {
                    registerTree(watchService, child);
                }

                // if source file is deleted check if there's matching
                // symlink, remove it with db element
                if (kind == ENTRY_DELETE) {
                    System.out.println("[ tracker ] " + kind + " | " + child);
                    // check if deleted element is file with matching extension
                    if (validExtension) {
                        removeQuery(filePath);
                        removeLink(filePath);
                    } else {
                        List<MediaQuery> queryByParentPath = mediaTrackerDao.findQueryByParentPath(child.toString());
                        for (MediaQuery mq : queryByParentPath) {
                            mediaTrackerDao.removeQueryFromQueue(mq);
                            System.out.println("[ tracker_delete ] removed queue entry: " +mq.getFilePath());
                        }
                        List<MediaLink> mediaLinkByFilePath = mediaTrackerDao.findMediaLinkByParentPath(child.toString());
                        for (MediaLink ml : mediaLinkByFilePath) {
                            mediaTrackerDao.removeLink(ml);
                            System.out.println("[ tracker_delete ] removed link: " + ml.getTargetPath());
                        }
                        // unregister element from watcher
                        eventPathKey.cancel();
                        watchKeyToPathMap.remove(eventPathKey);
                        pathToWatchKeyMap.remove(child);
                    }
                }

                // if newly created object is file
                if (kind == ENTRY_CREATE && validExtension) {
                    System.out.println("[ tracker ] " + kind.toString());
                    validateAndAdd(filePath);
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
    private void initialScan() {
        for (WatchKey watchKey : watchKeyToPathMap.keySet()) {
            Path path = watchKeyToPathMap.get(watchKey);
            String[] list = path.toFile().list();
            if (list != null) {
                for (String singleFile : list) {
                    if (validateExtension(singleFile)) {
                        String filePath = Path.of(path.toString(), singleFile).toString();
                        // check if file name already exists in db
                        MediaLink mediaLinkByFilePath = mediaTrackerDao.findMediaLinkByFilePath(filePath);
                        boolean matchingLink = mediaLinkByFilePath != null;
                        MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(filePath);
                        boolean matchingQuery = queryByFilePath != null;
                        if (!matchingLink && !matchingQuery) {
                            System.out.println("[ init ] found new file: " + filePath);
                            addNewQuery(filePath);
                        } else if (matchingLink && !matchingQuery) {
                            System.out.println("[ init ] existing link: ");
                        }
                    }
                }
            }
        }
    }

    private void validateAndAdd(String filePath) {
        if (validateExtension(filePath)) {
            // check if file name already exists in db
            MediaLink mediaLinkByFilePath = mediaTrackerDao.findMediaLinkByFilePath(filePath);
            boolean matchingLink = mediaLinkByFilePath != null;
            MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(filePath);
            boolean matchingQuery = queryByFilePath != null;
            if (!matchingLink && !matchingQuery) {
                System.out.println("[ init ] found new file: " + filePath);
                addNewQuery(filePath);
            } else if (matchingLink && !matchingQuery) {
                System.out.println("[ init ] existing link: ");
            }
        }
    }

    /*
     * Adds new query to queue
     * */
    private void addNewQuery(String filePath) {
        String parentPath = Path.of(filePath).getParent().toString();
        MediaQuery query = new MediaQuery(filePath, parentPath);
        mediaTrackerDao.addQueryToQueue(query);
        System.out.println("[ MediaQuery ] added to db with filepath: " + filePath);
    }

    /*
     *  Removes existing link
     * */
    private void removeLink(String filePath) {
        MediaLink mediaLinkByName = mediaTrackerDao.findMediaLinkByFilePath(filePath);
        if (mediaLinkByName != null) {
            System.out.println("[ remove_link ] Found matching link");
            mediaTrackerDao.removeLink(mediaLinkByName);
            System.out.println("[ remove_link ] Link deleted");
            String extractedParentPath = filePath.substring(0, filePath.lastIndexOf("\\"));
            if (cleanerService.isFolderEmpty(extractedParentPath)) cleanerService.deleteFolder(extractedParentPath);
        } else {
            System.out.println("[ remove_link ] No link found with this path");
        }
    }

    /*
     * Removes existing query
     * */
    private void removeQuery(String filePath) {
        MediaQuery queryByName = mediaTrackerDao.findQueryByFilePath(filePath);
        if (queryByName != null) {
            System.out.println("[ remove_query ] Found matching query");
            mediaTrackerDao.removeQueryFromQueue(queryByName);
            System.out.println("[ remove_query ] Query deleted");
        } else {
            System.out.println("[ remove_query ] No query found with this path");
        }
    }

//    public static void main(String[] args) throws IOException, InterruptedException {
//
//        MediaTrackerDao dao = new MediaTrackerDaoImpl();
//        String path = "./test-folder/";
//        List<Path> mediaFolder = List.of(Paths.get(path));
//        WatchService watchService = FileSystems.getDefault().newWatchService();
//        watch(watchService, mediaFolder, dao);
//
//
//
//    }
}

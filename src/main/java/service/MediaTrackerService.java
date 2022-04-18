package service;

import dao.MediaTrackerDao;
import model.MediaIgnored;
import model.MediaLink;
import model.MediaQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.CleanerService;
import util.MediaFilter;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;
import static util.MediaFilter.validateExtension;

public class MediaTrackerService {

    public static final Logger LOG = LoggerFactory.getLogger(MediaTrackerService.class);

    // pairing keys with paths
    private final Map<WatchKey, Path> watchKeyToPathMap = new HashMap<>();
    private final Map<Path, WatchKey> pathToWatchKeyMap = new HashMap<>();

    private final MediaTrackerDao mediaTrackerDao;
    private final CleanerService cleanerService;

    public MediaTrackerService(MediaTrackerDao dao, CleanerService cs) {
        mediaTrackerDao = dao;
        cleanerService = cs;
    }

    /*
     * Recursively add subdirectories of root directory to watch service.
     * If any file matching criteria is found while walking file tree,
     * it's automatically added to media queue.
     * */
    private void registerTree(WatchService watchService, Path root) throws IOException {

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

                watchKeyToPathMap.put(key, dir);
                pathToWatchKeyMap.put(dir, key);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (MediaFilter.validateExtension(file)) validateAndAdd(file.toString());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void watch(WatchService watchService, List<Path> paths) throws IOException, InterruptedException {
        /*
        * Get existing query list
        * */
        List<MediaQuery> beforeInitQueries = mediaTrackerDao.getAllMediaQueries();

        /*
        * Check against provided folders and delete entries that aren't child
        * elements of given paths.
        * */
        beforeInitQueries.stream()
                .filter(mq -> paths.stream().noneMatch(p -> Path.of(mq.getFilePath()).startsWith(p)))
                .forEach(this::removeQuery);

        for (Path path : paths) {
            // check if provided path exist and ignore invalid path
            if (pathValidator(path)) registerTree(watchService, path);
        }

        while (!watchKeyToPathMap.isEmpty()) {
            WatchKey watchKey = watchService.take();

            for (WatchEvent<?> event : watchKey.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path eventPath = (Path) event.context(); // event type
                /*
                 * Based on event watch key obtain path of directory in which
                 * event was observed.
                 * */
                Path parentDir = watchKeyToPathMap.get(watchKey);// get parentDir for specified event key
                /*
                 * Create full path for element that triggered event
                 * */
                Path child = parentDir.resolve(eventPath);

                boolean validExtension = validateExtension(eventPath.toString());
//                String filePath = Path.of(parentDir.toString(), eventPath.toString()).toString();
                String filePath = parentDir.resolve(eventPath).toString();

                /*
                 * If newly created element is directory add it
                 * and all of its children to watchlist.
                 * */
                if (kind == ENTRY_CREATE && Files.isDirectory(child)) {
                    registerTree(watchService, child);
                }

                // if source file is deleted check if there's matching
                // symlink, remove it with db element.
                if (kind == ENTRY_DELETE) {
                    /*
                     * If deleted element is a file with valid extension proceed
                     * to remove all the data pointing to it from database.
                     * */
                    if (validExtension) {
                        LOG.info("[ tracker ] {} | {}", kind, child);
                        removeQueryByFilePath(filePath);
                        removeLinkByFilePath(filePath);
                    } else {
                        LOG.info("[ tracker ] {} | {}", kind, child);
                        /*
                         * Get the event key for the element that triggered event.
                         * */
                        WatchKey eventPathKey = pathToWatchKeyMap.get(child); // get key for event element
                        /*
                         * If deleted element is directory, check if it contains any elements
                         * and if those elements match definition of media files.
                         * If so, proceed to remove all the data pointing to them from database.
                         * */
                        removeQueryByParentPath(child);
                        removeLinkByParentPath(child);
                        /*
                         * Check if deleted element has been watched and remove it from watchlist.
                         * Only folders will return non-null value.
                         * */
                        if (eventPathKey != null) {
                            watchKeyToPathMap.remove(eventPathKey);
                            pathToWatchKeyMap.remove(child);
                            eventPathKey.cancel();
                        }
                    }
                }
                // if newly created object is file
                if (kind == ENTRY_CREATE && validExtension) {
                    LOG.info("[ tracker ] {}", kind);
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
    * Check if provided path exists
    * */
    private boolean pathValidator(Path path) {
        return path.toFile().exists();
    }

    /*
    * Check if given path already exists either as link or as media query in database.
    * Otherwise, add it to query queue.
    * */
    private void validateAndAdd(String filePath) {
        MediaLink mediaLinkByFilePath = mediaTrackerDao.findMediaLinkByFilePath(filePath);
        boolean matchingLink = mediaLinkByFilePath != null;
        MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(filePath);
        boolean matchingQuery = queryByFilePath != null;
        MediaIgnored mediaIgnored = mediaTrackerDao.findMediaIgnoredByFilePath(filePath);
        boolean matchingIgnored = mediaIgnored != null;
        if (!matchingLink && !matchingQuery && !matchingIgnored) {
            LOG.info("[ init ] found new file: {}", filePath);
            addNewQuery(filePath);
        } else if (matchingLink) {
            LOG.warn("[ init ] existing link: {}", mediaLinkByFilePath);
        } else if (matchingIgnored) {
            LOG.warn("[ init ] file already ignored: {}", filePath);
        } else {
            LOG.warn("[ init ] already in queue: {}", filePath);
        }
    }

    /*
     * Adds new query to queue
     * */
    private void addNewQuery(String filePath) {
//        String parentPath = Path.of(filePath).getParent().toString();
        MediaQuery query = new MediaQuery(filePath);
        mediaTrackerDao.addQueryToQueue(query);
        LOG.info("[ new_query ] added to the queue with filepath: {}", filePath);

    }

    /*
     * Removes existing link from database, deletes symlink and containing folder.
     * */
    private void removeLink(MediaLink mediaLink) {
        mediaTrackerDao.removeLink(mediaLink.getMediaId());
        LOG.info("[ remove_link ] removed link: {}", mediaLink);
        cleanerService.deleteElement(Path.of(mediaLink.getLinkPath()));
        LOG.info("[ remove_link ] file deleted: {}", mediaLink.getLinkPath());
        Path linkParentPath = Path.of(mediaLink.getLinkPath()).getParent();
        if (cleanerService.containsNoMediaFiles(linkParentPath)) {
            cleanerService.deleteElement(linkParentPath);
        }
    }

    /*
     * Removes existing query
     * */
    private void removeQuery(MediaQuery mediaQuery) {
        mediaTrackerDao.removeQueryFromQueue(mediaQuery);
        LOG.info("[ remove_query ] query deleted: {}", mediaQuery);
    }

    private void removeLinkByParentPath(Path child) {
        String phrase = child.getName(child.getNameCount() - 1).toString();
        List<MediaLink> mediaLinkByFilePath = mediaTrackerDao.findInTargetFilePathLink(phrase);
        for (MediaLink ml : mediaLinkByFilePath) {
            removeLink(ml);
            LOG.info("[ remove_link ] removed link: {}", ml.getLinkPath());
        }
    }

    private void removeQueryByParentPath(Path child) {
        String phrase = child.getName(child.getNameCount() - 1).toString();
        List<MediaQuery> queryByParentPath = mediaTrackerDao.findInFilePathQuery(phrase);
        for (MediaQuery mq : queryByParentPath) {
            mediaTrackerDao.removeQueryFromQueue(mq);
            LOG.info("[ remove_query ] removed queue entry: {}", mq.getFilePath());
        }
    }

    private void removeLinkByFilePath(String filePath) {
        MediaLink mediaLinkByFilePath = mediaTrackerDao.findMediaLinkByFilePath(filePath);
        if (mediaLinkByFilePath != null) {
            LOG.info("[ remove_link ] Found matching link");
            removeLink(mediaLinkByFilePath);
        } else {
            LOG.info("[ remove_link ] No link found with this path");
        }
    }

    private void removeQueryByFilePath(String filePath) {
        MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(filePath);
        if (queryByFilePath != null) {
            LOG.info("[ remove_query ] Found matching query");
            removeQuery(queryByFilePath);
        } else {
            LOG.info("[ remove_query ] No query found with this path");
        }
    }

}

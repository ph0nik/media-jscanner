package tracker;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.MediaQuery;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;
import static util.MediaFilter.*;

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

    private static void watch(WatchService watchService, Path path, MediaTrackerDao dao) throws IOException, InterruptedException {
        registerTree(watchService, path);

        while (true) {
            WatchKey watchKey = watchService.take();

            for (WatchEvent<?> event : watchKey.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path eventPath = (Path) event.context(); // event type
                Path directory = watchKeyToPathMap.get(watchKey); // directory for specified event key
                Path child = directory.resolve(eventPath);

                String tempFileName = eventPath.toString();
                boolean validExtension = validateExtension(tempFileName);
                String filePath = Path.of(directory.toString(), eventPath.toString()).toString();

                // if newly created object is directory add it to watchlist
                if (kind == ENTRY_CREATE && Files.isDirectory(child)) {
                    registerTree(watchService, child);
                }

                // if source file is deleted check if there's matching
                // symlink, remove it with db element
                if (kind == ENTRY_DELETE && validExtension) {
                    System.out.println(kind.toString());
                    MediaQuery queryByName = dao.findQueryByName(tempFileName);
                    if (queryByName != null) {
                        System.out.println("Found mathing query");
                        dao.removeQueryFromQueue(queryByName);
                        System.out.println("Query deleted");
                    }
                }

                // if newly created object is file
                if (kind == ENTRY_CREATE && validExtension) {
                    System.out.println("Type " + kind.toString());
                    System.out.println("Valid media.");
                    MediaQuery query = new MediaQuery(tempFileName, filePath);
                    dao.addQueryToQueue(query);
                    System.out.println("Query added to db.");
                    System.out.println(filePath);

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

    public static void main(String[] args) throws IOException, InterruptedException {

        MediaTrackerDao dao = new MediaTrackerDaoImpl();
        String path = "./test-folder/";
        Path mediaFolder = Paths.get(path);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        watch(watchService, mediaFolder, dao);


    }
}

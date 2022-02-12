package main.tracker;

import main.model.Query;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;
import static main.util.MediaFilter.*;

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

    private static void watch(WatchService watchService, Path path) throws IOException, InterruptedException {
        registerTree(watchService, path);

//        boolean valid = true;
        while (true) {
            WatchKey watchKey = watchService.take();

            for (WatchEvent<?> event : watchKey.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path eventPath = (Path) event.context(); // event type
                Path directory = watchKeyToPathMap.get(watchKey); // directory for specified event key
                Path child = directory.resolve(eventPath);

                // if newly created object is directory add it to watchlist
                if (kind == ENTRY_CREATE && Files.isDirectory(child)) {
                    registerTree(watchService, child);
                }

                if (kind == ENTRY_DELETE) {
                    System.out.println(kind.toString());
                }

                // if newly created object is file
                if (kind == ENTRY_CREATE) {
                    System.out.println("Type " + kind.toString());
                    String tempFileName = eventPath.toString();
                    if (validateExtension(tempFileName)) {
                        String fileNameWithoutExt = getFileName(tempFileName);
                        Query query = new Query(fileNameWithoutExt, tempFileName);

                        // create media obj
                        // add to db
                        System.out.println(Path.of(directory.toString(), eventPath.toString()));
                    }
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

        String path = "./test-folder/";
        Path mediaFolder = Paths.get(path);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        watch(watchService, mediaFolder);



    }
}

package tracker;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;


public class MediaTracker {

    // pairing keys with paths
    private static Map<WatchKey, Path> watchKeyToPathMap;

    public MediaTracker() {
        watchKeyToPathMap = new HashMap<>();
    }

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

    public static void main(String[] args) throws IOException, InterruptedException {

        String path = "e:/Temp/";

        Path mediaFolder = Paths.get(path);

        WatchService watchService = FileSystems.getDefault().newWatchService();
        mediaFolder.register(watchService, ENTRY_CREATE, ENTRY_DELETE);

        boolean valid = true;
        do {
            WatchKey watchKey = watchService.take();

            for (WatchEvent event : watchKey.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                if (ENTRY_CREATE.equals(kind)) {
                    System.out.println(event.context().toString() + " created");
                }
                if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
                    System.out.println(event.context().toString() + " deleted");
                }
                valid = watchKey.reset();
            }
        } while (valid);

    }
}

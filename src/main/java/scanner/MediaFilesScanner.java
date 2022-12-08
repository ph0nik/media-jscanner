package scanner;

import dao.MediaTrackerDao;
import model.MediaLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import util.CleanerService;
import util.MediaFilter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Component
public class MediaFilesScanner {

    public static final Logger LOG = LoggerFactory.getLogger(MediaFilesScanner.class);

    private final MediaTrackerDao mediaTrackerDao;
    private final CleanerService cleanerService;
//    private List<MediaLink> allMediaIgnored;
    private List<MediaLink> allMediaLinks;
    private List<Path> candidateFilesList;

    public MediaFilesScanner(@Qualifier("spring") MediaTrackerDao dao, CleanerService cs) {
        this.mediaTrackerDao = dao;
        this.cleanerService = cs;
    }

    public List<Path> scanMediaFolders(List<Path> paths, List<MediaLink> allMediaLinks) {
        this.allMediaLinks = allMediaLinks;
        candidateFilesList = new LinkedList<>();
        for (Path p : paths) {
            scanFolderTree(p);
        }
        return candidateFilesList;
    }

    void scanFolderTree(Path root) {
        cleanUp();
        try {
            Files.walkFileTree(root, new MediaFilesFileVisitor());
        } catch (IOException e) {
            LOG.error("[ scan ] Error: {}", e.getMessage());
        }
//        extractQueryList();
    }

    void cleanUp() {
        // remowing links pointing to non existing files
//        cleanerService.deleteInvalidLink(allMediaLinks, mediaTrackerDao);
        cleanerService.deleteInvalidIgnoredMedia(mediaTrackerDao);
    }

    // write file list for test
    public void extractQueryList() throws IOException {
        Path targetFile = Path.of("R:\\!_out.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
            for (Path p : candidateFilesList) {
                writer.write(p.toString() + "\n");
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

    }

    /*
    * Checks if given path matches any paths already in database
    * */
    boolean containsPath(Path path) {
        return allMediaLinks.stream().anyMatch(x -> x.getOriginalPath().equals(path.toString()));
    }

    class MediaFilesFileVisitor implements FileVisitor<Path> {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (MediaFilter.validateExtension(file) && !containsPath(file)) {
                candidateFilesList.add(file);
                LOG.info("[ file_walk ] new path: {}", file);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }



}

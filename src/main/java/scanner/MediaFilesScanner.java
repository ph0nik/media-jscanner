package scanner;

import dao.MediaTrackerDao;
import model.MediaIgnored;
import model.MediaLink;
import model.MediaQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.CleanerService;
import util.MediaFilter;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MediaFilesScanner {

    public static final Logger LOG = LoggerFactory.getLogger(MediaFilesScanner.class);

    private final MediaTrackerDao mediaTrackerDao;
    private final CleanerService cleanerService;
    private List<MediaIgnored> allMediaIgnored;
    private List<MediaLink> allMediaLinks;
    private LinkedList<Path> candidateFilesList;
    private List<MediaQuery> mediaQueries;

    public MediaFilesScanner(MediaTrackerDao dao, CleanerService cs) {
        this.mediaTrackerDao = dao;
        this.cleanerService = cs;
    }

    public List<MediaQuery> scanMediaFolders(List<Path> paths) throws IOException {
        loadMediaLists();
        mediaQueries = new LinkedList<>();
        for (Path p : paths) {
            scanFolderTree(p);
        }
        return mediaQueries;
    }

    void scanFolderTree(Path root) throws IOException {
        cleanUp();
        candidateFilesList = new LinkedList<>();
        Files.walkFileTree(root, new MediaFilesFileVisitor());

        for (Path p : candidateFilesList) {
            MediaQuery mediaQuery = new MediaQuery(p.toString());
            mediaQuery.setQueryUuid(UUID.randomUUID());
            mediaQueries.add(mediaQuery);
        }
    }

    void loadMediaLists() {
        allMediaIgnored = mediaTrackerDao.getAllMediaIgnored();
        allMediaLinks = mediaTrackerDao.getAllMediaLinks();
    }

    void cleanUp() {
        cleanerService.deleteInvalidLink(allMediaLinks, mediaTrackerDao);
        cleanerService.deleteInvalidIgnoredMedia(allMediaIgnored, mediaTrackerDao);
    }

    boolean containsPath(Path path) {
        boolean aml = allMediaLinks.stream().anyMatch(x -> x.getTargetPath().equals(path.toString()));
        boolean ami = allMediaIgnored.stream().anyMatch(x -> x.getTargetPath().equals(path.toString()));
        return aml || ami;
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

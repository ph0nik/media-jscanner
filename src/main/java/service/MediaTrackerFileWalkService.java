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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class MediaTrackerFileWalkService {

    public static final Logger LOG = LoggerFactory.getLogger(MediaTrackerFileWalkService.class);

    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;
    private List<MediaIgnored> allMediaIgnored;
    private List<MediaLink> allMediaLinks;
    private List<MediaQuery> allMediaQueries;
    private LocalDateTime fileWalkServiceUpdateTime;
    private LinkedList<Path> candidateFilesList;


    public MediaTrackerFileWalkService(MediaTrackerDao dao, CleanerService cs) {
        this.mediaTrackerDao = dao;
        this.cleanerService = cs;
    }
    public void scanMediaFolders(List<Path> paths) throws IOException, InterruptedException {
        for (Path p : paths) {
            scanFolderTree(p);
        }
    }

    void scanFolderTree(Path root) throws IOException {
//        if (allMediaQueries == null
//                || fileWalkServiceUpdateTime.isBefore(MediaLinksServiceLastUpdate.getLatestUpdateTime()))
//            loadMediaLists();
        if (allMediaQueries == null) loadMediaLists();

        cleanerService.deleteInvalidMediaQuery(allMediaQueries, mediaTrackerDao);
        candidateFilesList = new LinkedList<>();
        Files.walkFileTree(root, new MediaFilesFileVisitor());

        for (Path p : candidateFilesList) {
            addNewFileToQueue(p);
        }
    }

    void loadMediaLists() {
        allMediaIgnored = mediaTrackerDao.getAllMediaIgnored();
        allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        allMediaQueries = mediaTrackerDao.getAllMediaQueries();
    }


    void addNewFileToQueue(Path path) {
        MediaQuery mediaQuery = new MediaQuery();
        mediaQuery.setFilePath(path.toString());
        mediaTrackerDao.addQueryToQueue(mediaQuery);
        LOG.info("[ file_walk ] Added to queue: {}", mediaQuery);
        fileWalkServiceUpdateTime = LocalDateTime.now();
    }

    boolean containsPath(Path path) {
        boolean amq = allMediaQueries.stream().anyMatch(x -> x.getFilePath().equals(path.toString()));
        boolean aml = allMediaLinks.stream().anyMatch(x -> x.getTargetPath().equals(path.toString()));
        boolean ami = allMediaIgnored.stream().anyMatch(x -> x.getTargetPath().equals(path.toString()));
        return amq || aml || ami;
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

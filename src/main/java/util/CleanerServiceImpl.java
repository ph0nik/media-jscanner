package util;

import dao.MediaTrackerDao;
import model.MediaLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CleanerServiceImpl implements CleanerService {

    private static final Logger LOG = LoggerFactory.getLogger(CleanerServiceImpl.class);

//    MediaLink{mediaId=1,
//    sourcePath='G:\Java\media-jscanner\test-folder\movies-source\Memories of Matsuko (2006) [tmdbid-31512]\Memories of Matsuko-cd1.avi',
//    destPath='.\test-folder\movies-target\Memories of Matsuko-Kiraware Matsuko no Isshō\wrd-matsuko-cd1.avi',
//    theMovieDbId='31512', sourceParentPath='null'}

    public void deleteEmptyFolders(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    File[] files = dir.toFile().listFiles();
                    if (files != null && files.length == 0) deleteElement(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

//    @Override
//    public void deleteInvalidMediaQuery(List<MediaQuery> queries, MediaTrackerDao dao) {
//        for (MediaQuery mq : queries) {
//            File file = new File(mq.getFilePath());
//            if (!file.exists()) {
//                dao.removeQueryFromQueue(mq);
//                LOG.info("[ cleaner ] Invalid queue element deleted: {}", mq);
//            }
//        }
//    }

    /*
    * Removes ignored elements that are connected with non-existing files
    * */
    @Override
    public void deleteInvalidIgnoredMedia(MediaTrackerDao dao) {
        // TODO move filter elsewhere
        List<MediaLink> mediaIgnoredList = dao.getAllMediaLinks().stream().filter(mi -> mi.getLinkPath().equals("ignore")).collect(Collectors.toList());
        int count = 0;
        for (MediaLink ml : mediaIgnoredList) {
//            Files.exists(Path.of(ml.getOriginalPath()));
//            File file = new File(ml.getOriginalPath());
            if (Files.exists(Path.of(ml.getOriginalPath()))) {
                dao.removeLink(ml.getMediaId());
                count++;
                LOG.info("[ cleaner ] Invalid ignored media deleted: {}", ml);
            }
        }
        LOG.info("[ cleaner ] {} elements removed", count);
    }

    public boolean containsNoMediaFiles(Path targetPath) throws IOException {
            return Files.walk(targetPath)
                    .noneMatch(MediaFilter::validateExtension);
    }

    @Override
    public boolean deleteElement(Path linkPath) {
        boolean res = false;
        try (Stream<Path> stream = Files.walk(linkPath)) {
            res = stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .allMatch(File::delete);
        } catch (IOException exception) {
            LOG.error("[ delete_element ] Error: {}", exception.getMessage());
        }
        return res;
    }

    @Override
    public void clearParentFolder(Path path) throws IOException {
        if (!path.toFile().isDirectory()) {
            Path parent = path.getParent();
            if (containsNoMediaFiles(parent)) {
                deleteElement(parent);
            }
        } else {
            LOG.error("[ cleaner ] Given path is not a file: {}", path);
        }
    }

    @Override
    public int deleteInvalidDbEntries(MediaTrackerDao mediaTrackerDao) {
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        int counter = 0;
        for (MediaLink ml : allMediaLinks) {
            boolean originalExists = Path.of(ml.getOriginalPath()).toFile().exists();
            boolean linkExists = Path.of(ml.getLinkPath()).toFile().exists();
            if (!originalExists && !linkExists) {
                mediaTrackerDao.removeLink(ml.getMediaId());
                counter++;
            }
        }
        LOG.info("[ clean_invalid_entries ] {} invalid entries deleted", counter);
        return counter;
    }

    public static void main(String[] args) {
        Path path = Path.of("E:\\Temp\\links-folder\\Bewitched (1981) [imdbid-tt0082481]\\folder.jpg");
        CleanerService cs = new CleanerServiceImpl();
        cs.deleteElement(path);
    }

}

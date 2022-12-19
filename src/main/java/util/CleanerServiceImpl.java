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
import java.util.stream.Stream;

@Service
public class CleanerServiceImpl implements CleanerService {

    private static final Logger LOG = LoggerFactory.getLogger(CleanerServiceImpl.class);

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

    /*
     * Removes ignored elements that are connected with non-existing files
     * */
//    @Override
//    public void deleteInvalidIgnoredMedia(MediaTrackerDao dao) {
//        List<MediaLink> mediaIgnoredList = dao.getAllMediaLinks().stream().filter(mi -> mi.getLinkPath().equals("ignore")).collect(Collectors.toList());
//        int count = 0;
////        mediaIgnoredList.stream().map(ml -> Files.exists(Path.of(ml.getOriginalPath())))
//        for (MediaLink ml : mediaIgnoredList) {
////            Files.exists(Path.of(ml.getOriginalPath()));
////            File file = new File(ml.getOriginalPath());
//            if (!Files.exists(Path.of(ml.getOriginalPath()))) {
//                dao.removeLink(ml.getMediaId());
//                count++;
//                LOG.info("[ cleaner ] Invalid ignored media deleted: {}", ml);
//            }
//        }
//        LOG.info("[ cleaner ] {} elements removed", count);
//    }

    public boolean containsNoMediaFiles(Path targetPath) {
        boolean result = false;
        try (Stream<Path> stream = Files.walk(targetPath)) {
            result = stream.noneMatch(MediaFilter::validateExtension);
        } catch (IOException e) {
            LOG.error("[ media_check ] Error: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public void deleteInvalidLink(MediaLink mediaLink, MediaTrackerDao dao) {
        dao.removeLink(mediaLink.getMediaId());
        LOG.info("[ link_delete ] Link with id: {} removed", mediaLink.getMediaId());
        deleteElement(Path.of(mediaLink.getLinkPath()));
        clearParentFolder(Path.of(mediaLink.getLinkPath()));
    }

    @Override
    public boolean deleteElement(Path linkPath) {
        boolean res = false;
        try (Stream<Path> stream = Files.walk(linkPath)) {
            res = stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .allMatch(f -> deleteResult(f, f.delete()));
        } catch (IOException exception) {
            LOG.error("[ delete_element ] Error: {}", exception.getMessage());
        }
        return res;
    }

    boolean deleteResult(File f, boolean b) {
        if (b) LOG.info("[ element_delete ] File {} deleted successfully", f);
        else LOG.warn("[ element_delete ] File {} already in use", f);
        return b;
    }

    /*
    * Deletes empty directories.
    * Directory is considered empty if it contains no directory and no media files.
    * */
    @Override
    public void clearEmptyFolders(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(Files::isDirectory)
                    .filter(this::containsNoMediaFiles)
                    .forEachOrdered(this::deleteElement);
        } catch (IOException exception) {
            LOG.error("[ delete_element ] Error: {}", exception.getMessage());
        }
    }

    @Override
    public void clearParentFolder(MediaLink mediaLink) {
        clearParentFolder(Path.of(mediaLink.getLinkPath()).getParent());
    }

    @Override
    public void clearParentFolder(Path parent) {
        if (parent.toFile().isDirectory()) {
            try (Stream<Path> s = Files.list(parent)) {
                if (containsNoMediaFiles(parent) && s.noneMatch(Files::isDirectory)) {
                    deleteElement(parent);
                    LOG.info("[ cleaner ] No media files found, folder: {} deleted.", parent);
                }
            } catch (IOException e) {
                LOG.error("[ cleaner ] {}", e.getMessage());
            }
        } else {
            LOG.error("[ cleaner ] Given path is not a folder: {}", parent);
        }
    }

    @Override
    public int deleteInvalidDbEntries(MediaTrackerDao mediaTrackerDao) {
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        int counter = 0;
        for (MediaLink ml : allMediaLinks) {
            boolean originalExists = Path.of(ml.getOriginalPath()).toFile().exists();
            boolean linkExists = Path.of(ml.getLinkPath()).toFile().exists();
            if ((!linkExists && originalExists) || (!originalExists && !linkExists)) {
                mediaTrackerDao.removeLink(ml.getMediaId());
                counter++;
            }
        }
        LOG.info("[ clean_invalid_entries ] {} invalid entries deleted", counter);
        return counter;
    }

    public static void main(String[] args) {
        String s = "G:\\Java\\media-jscanner\\test-folder\\movies-incoming\\";
        CleanerServiceImpl cs = new CleanerServiceImpl();
        cs.clearEmptyFolders(Path.of(s));
    }
}

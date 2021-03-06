package util;

import dao.MediaTrackerDao;
import model.MediaIgnored;
import model.MediaLink;
import model.MediaQuery;
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

    @Override
    public void deleteInvalidMediaQuery(List<MediaQuery> queries, MediaTrackerDao dao) {
        for (MediaQuery mq : queries) {
            File file = new File(mq.getFilePath());
            if (!file.exists()) {
                dao.removeQueryFromQueue(mq);
                LOG.info("[ cleaner ] Invalid queue element deleted: {}", mq);
            }
        }
    }

    @Override
    public void deleteInvalidLink(List<MediaLink> links, MediaTrackerDao dao) {
        for(MediaLink ml : links) {
            File file = new File(ml.getTargetPath());
            if (!file.exists()) {
                dao.removeLink(ml.getMediaId());
                LOG.info("[ cleaner ] Invalid link element deleted: {}", ml);
            }
        }
    }

    @Override
    public void deleteInvalidIgnoredMedia(List<MediaIgnored> mediaIgnoredList, MediaTrackerDao dao) {
        for (MediaIgnored mi : mediaIgnoredList) {
            File file = new File(mi.getTargetPath());
            if (!file.exists()) {
                dao.removeMediaIgnored(mi.getMediaId());
                LOG.info("[ cleaner ] Invalid ignored media deleted: {}", mi);
            }
        }
    }

    /*
     * Get all media links from db, iterate through links folder and
     * check if all directories match links from db.
     * Delete any that are not present.
     * */
    public void deleteInvalidLinks(Path root, MediaTrackerDao dao) {
        File[] files = root.toFile().listFiles();
        if (files != null) {
            for (File f : files) {
                List<MediaLink> inFilePathLink = dao.findInLinkPathLink(f.toString());
                if (inFilePathLink.isEmpty()) {
                    deleteElement(f.toPath());
                    LOG.info("[ cleaner ] invalid link deleted: {}", f);
                }
            }
        }
    }

    public boolean containsNoMediaFiles(Path targetPath) {
        try {
            return Files.walk(targetPath)
                    .noneMatch(MediaFilter::validateExtension);
        } catch (IOException e) {
            LOG.error("[ cleaner ] {}", e.getMessage());
        }
        return false;
    }

    @Override
    public void deleteElement(Path linkPath) {
        try {
            Files.walk(linkPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            LOG.error("[ cleaner ] {}", e.getMessage());
        }
    }

    @Override
    public void clearParentFolder(Path path) {
        if (!path.toFile().isDirectory()) {
            Path parent = path.getParent();
            if (containsNoMediaFiles(parent)) {
                deleteElement(parent);
            }
        } else {
            LOG.error("[ cleaner ] Given path is not a file: {}", path);
        }

    }

//    public void deleteNonMediaFiles(Path path) {
//        File directory = path.toFile();
//        String[] contents = directory.list();
//        if (contents != null) {
//            for (String s : contents) {
//                deleteElement(directory.toPath().resolve(s));
//            }
//        }
//    }

}

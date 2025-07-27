package util;

import dao.MediaTrackerDao;
import model.MediaLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CleanerServiceImpl implements CleanerService {

    private static final Logger LOG = LoggerFactory.getLogger(CleanerServiceImpl.class);

    /*
     * Checks if given directory contains files with declared extensions.
     * If no files match the criteria, path does not exist or path is not a folder returns true.
     * Otherwise, returns false.
     * */
    public boolean containsNoMediaFiles(Path targetPath) {
        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
            LOG.error("[ media_check ] Non existing path or not a folder.");
            return true;
        }
        try (Stream<Path> stream = Files.walk(targetPath)) {
            return stream
                    .noneMatch(MediaFilter::validateExtension);
        } catch (IOException e) {
            LOG.error("[ media_check ] Error: {}", e.getMessage());
        }
        return false;
    }

    /*
     * With given path deletes every file and folder withing this path
     * */
    @Override
    public void deleteElements(Path linkPath) {
        try (Stream<Path> stream = Files.walk(linkPath)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEachOrdered(this::deleteSingleFile);
//                    .collect(Collectors.toMap(p -> p, this::deleteSingleFile));
        } catch (IOException exception) {
            LOG.error("[ delete_element ] Error: {}", exception.getMessage());
        }
//        return Map.of();
    }

    @Override
    public boolean deleteSingleFile(Path path) {
        try {
            return deleteResult(
                    path,
                    Files.isDirectory(path),
                    Files.deleteIfExists(path)
            );
        } catch (IOException e) {
            LOG.error("[ delete_element ] File: {}, Error:{}", path, e.getMessage());
        }
        return false;
    }

    boolean deleteResult(Path f, boolean isFolder, boolean b) {
        String s = (isFolder) ? "directory" : "file";
        if (b) LOG.info("[ element_delete ] {} deleted: {}", s, f);
        else LOG.warn("[ element_delete ] {} doesn't exist or in use: {}", s, f);
        return b;
    }

    /*
     * Deletes empty directories.
     * Directory is considered empty if it contains no directory and no media files.
     * */
    @Override
    public void clearEmptyFolders(Path path) {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            LOG.error("[ clear_folder ] Element not found or not a folder: {} ", path);
        } else if (Files.isDirectory(path) && containsNoMediaFiles(path)) {
            LOG.info("[ clear_folder ] No media files found.");
            deleteElements(path);
        } else {
            LOG.info("[ clear_folder ] Folder contains media files.");
        }
    }

    @Override
    public List<Path> checkPathForClearing(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return List.of();
        List<Path> output = new LinkedList<>();
        for (Path p : paths) {
            if (Files.isDirectory(p) && containsNoMediaFiles(p)) {
                LOG.info("[ clear_folder ] {} ... No media files found.", p);
                output.add(p);
            } else {
                LOG.info("[ clear_folder ] {} ... Folder contains media files.", p);
            }
        }
        return output;
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
}

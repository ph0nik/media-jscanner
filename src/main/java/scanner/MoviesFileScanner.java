package scanner;

import model.MediaLink;
import model.path.FilePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MoviesFileScanner implements MediaFilesScanner {

    public static final Logger LOG = LoggerFactory.getLogger(MoviesFileScanner.class);

    public List<Path> scanMediaFolders(List<FilePath> sourcePaths, List<MediaLink> existingMediaLinks) {
//        this.allMediaLinks = existingMediaLinks;
//        List<Path> candidateFilesList = new LinkedList<>();
        return sourcePaths.stream()
                .flatMap(p -> scanFolderTree(p.getPath(), existingMediaLinks).stream())
                .collect(Collectors.toList());
//        for (FilePath p : sourcePaths) {
//            candidateFilesList.addAll(scanFolderTree(p.getPath(), existingMediaLinks));
//        }
//        return candidateFilesList;
    }

    List<Path> scanFolderTree(Path root, List<MediaLink> existingMediaLinks) {
        NewFileLister newMoviesFiles = new NewFileLister(existingMediaLinks);
        try {
            LOG.info("[ scan ] Scanning folder: {}", root);
            Files.walkFileTree(root, newMoviesFiles);
        } catch (IOException e) {
            LOG.error("[ scan ] Error: {}", e.getMessage());
        }
        return newMoviesFiles.getCurrentCandidates();
    }

    // write file list for test
    public void extractQueryList(List<Path> candidateFilesList) {
        Path targetFile = Path.of("R:\\!_out.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
            for (Path p : candidateFilesList) {
                writer.write(p.toString() + "\n");
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

}

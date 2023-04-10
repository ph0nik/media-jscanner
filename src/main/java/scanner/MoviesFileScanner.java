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
import java.util.LinkedList;
import java.util.List;

@Component
public class MoviesFileScanner implements MediaFilesScanner {

    public static final Logger LOG = LoggerFactory.getLogger(MoviesFileScanner.class);
    private List<MediaLink> allMediaLinks;

    public MoviesFileScanner() {
    }

    public List<Path> scanMediaFolders(List<FilePath> paths, List<MediaLink> allMediaLinks) {
        this.allMediaLinks = allMediaLinks;
        List<Path> candidateFilesList = new LinkedList<>();
        for (FilePath p : paths) {
            candidateFilesList.addAll(scanFolderTree(p.getPath()));
        }
        return candidateFilesList;
    }

    List<Path> scanFolderTree(Path root) {
        NewFileLister newMoviesFiles = new NewFileLister(allMediaLinks);
        try {
            LOG.info("[ scan ] Movie scanner started...");
            Files.walkFileTree(root, newMoviesFiles);
        } catch (IOException e) {
            LOG.error("[ scan ] Error: {}", e.getMessage());
        }
        return newMoviesFiles.getCurrentCandidates();
    }

    // write file list for test
    public void extractQueryList(List<Path> candidateFilesList) throws IOException {
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

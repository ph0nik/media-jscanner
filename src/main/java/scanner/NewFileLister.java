package scanner;

import model.MediaLink;
import util.MediaFilter;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

public class NewFileLister implements FileVisitor<Path> {

    private final List<MediaLink> existingMediaLinks;
    private final List<Path> candidateFilesList;

    public NewFileLister(List<MediaLink> existingMediaLinksList) {
        existingMediaLinks = existingMediaLinksList;
        candidateFilesList = new LinkedList<>();
    }

    public List<Path> getCurrentCandidates() {
        return candidateFilesList;
    }

    /*
     * Checks if given path matches any paths already in database
     * */
    boolean containsPath(Path path) {
        return existingMediaLinks.stream()
                .anyMatch(x -> x.getOriginalPath().equals(path.toString()));
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (MediaFilter.validateExtension(file) && !containsPath(file)) {
            candidateFilesList.add(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        return FileVisitResult.CONTINUE;
    }
}


package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class CleanerServiceImpl implements CleanerService {

//    MediaLink{mediaId=1,
//    sourcePath='G:\Java\media-jscanner\test-folder\movies-source\Memories of Matsuko (2006) [tmdbid-31512]\Memories of Matsuko-cd1.avi',
//    destPath='.\test-folder\movies-target\Memories of Matsuko-Kiraware Matsuko no Isshō\wrd-matsuko-cd1.avi',
//    theMovieDbId='31512', sourceParentPath='null'}
    // TODO cleaning folders with invalid links

    public void cleanUpLinkFolder(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {

                    if (containsNoMediaFiles(dir)) {
                        File[] files = dir.toFile().listFiles();
                        if (files != null) {
                            for (File f : files) {
                                deleteElement(f.toPath());
                            }
                        }
                        deleteElement(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // TODO catch
            e.printStackTrace();
        }
    }

    public boolean containsNoMediaFiles(Path targetPath) {
        File directoryPath = targetPath.toFile();
        String[] contents = directoryPath.list();
        if (contents != null) {
            for (String s : contents) {
                if (MediaFilter.validateExtension(s)) return false;
            }
        }
        return true;
    }

    @Override
    public void deleteElement(Path linkPath) {
        try {
            Files.delete(linkPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[ cleaner ] " + e.getMessage());
        }
    }

    public void deleteNonMediaFiles(Path path) {
        File directory = path.toFile();
        String[] contents = directory.list();
        if (contents != null) {
            for (String s : contents) {
                deleteElement(directory.toPath().resolve(s));
            }
        }
    }
}

package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CleanerServiceImpl implements CleanerService {

//    MediaLink{mediaId=1,
//    sourcePath='G:\Java\media-jscanner\test-folder\movies-source\Memories of Matsuko (2006) [tmdbid-31512]\Memories of Matsuko-cd1.avi',
//    destPath='.\test-folder\movies-target\Memories of Matsuko-Kiraware Matsuko no Isshō\wrd-matsuko-cd1.avi',
//    theMovieDbId='31512', sourceParentPath='null'}
    // TODO cleaning folders with invalid links


    public boolean containsMediaFiles(String targetPath) {
        File directoryPath = new File(targetPath);
        String[] contents = directoryPath.list();
        if (contents != null) {
            for (String s : contents) {
                if (MediaFilter.validateExtension(s)) return false;
            }
        }
        return true;
    }

    @Override
    public void deleteElement(String linkPath) {
        Path path = Path.of(linkPath);
        try {
            Files.delete(path);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[ cleaner ] " + e.getMessage());
        }
    }

    public void deleteNonMediaFiles(String path) {
        File directory = new File(path);
        String[] contents = directory.list();
        if (contents != null) {
            for (String s : contents) {
                deleteElement(directory.toPath().resolve(s).toString());
            }
        }
    }
}

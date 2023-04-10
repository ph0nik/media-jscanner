package scanner;

import model.MediaLink;
import model.path.FilePath;

import java.nio.file.Path;
import java.util.List;

public interface MediaFilesScanner {

    public List<Path> scanMediaFolders(List<FilePath> paths, List<MediaLink> allMediaLinks);

}

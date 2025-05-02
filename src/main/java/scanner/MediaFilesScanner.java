package scanner;

import model.MediaLink;

import java.nio.file.Path;
import java.util.List;

public interface MediaFilesScanner {

    public List<Path> scanMediaFolders(List<Path> paths, List<MediaLink> allMediaLinks);

}

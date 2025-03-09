package service.backup;

import model.MediaLink;
import service.exceptions.MissingFolderOrFileException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface BackupService {
    /*
    * Exports provided list of MediaLink objects as XML file.
    * Created filename is based of current date and time.
    * Returns filename.
    * */
    String exportRecords(Path dataDirectory, List<MediaLink> mediaLinkList) throws IOException, MissingFolderOrFileException; //TODO

    /*
    * Imports list of MediaLink objects from a file.
    * */
    List<MediaLink> importRecords(Path fileName) throws IOException, MissingFolderOrFileException;

    /*
    * Checks if data folder is present.
    * */
    boolean checkAppDataFolder(Path dataDirectory);
}

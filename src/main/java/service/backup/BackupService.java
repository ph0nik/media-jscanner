package service.backup;

import model.MediaLink;
import service.backup.model.BackupXml;

import java.io.IOException;
import java.util.List;

public interface BackupService {

    BackupXml prepareItemsForBackup(List<MediaLink> mediaLinks);

    /*
    * Exports provided list of MediaLink objects as XML file.
    * Created filename is based of current date and time.
    * Returns filename.
    * */
    String exportRecords(List<MediaLink> mediaLinkList) throws IOException; //TODO

    /*
    * Imports list of MediaLink objects from a file.
    * */
    List<MediaLink> importRecords(String fileName) throws IOException;

    /*
    * Checks if data folder is present.
    * */
    boolean checkAppDataFolder();
}

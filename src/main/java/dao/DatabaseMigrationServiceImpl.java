package dao;

import model.MediaLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import service.backup.XmlBackupService;
import service.exceptions.MissingFolderOrFileException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class DatabaseMigrationServiceImpl implements DatabaseMigrationService {

//    private final String backupFolder = "data/";

    @Qualifier("spring")
    @Autowired
    MediaTrackerDao mediaTrackerDao;

    @Autowired
    XmlBackupService xmlBackupService;

    @Override
    public String backupDatabase(Path backupFolder) throws IOException, MissingFolderOrFileException {
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        return xmlBackupService.exportRecords(backupFolder, allMediaLinks);
    }

    @Override
    public void restoreDatabase(Path backupFolder, String backupFileName) throws IOException, MissingFolderOrFileException {
        Path restoreFilePath = backupFolder.resolve(Path.of(backupFileName));
        xmlBackupService.importRecords(restoreFilePath);
    }

}

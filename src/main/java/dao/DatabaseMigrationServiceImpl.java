package dao;

import model.MediaLink;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import service.PropertiesService;
import service.backup.XmlBackupService;
import service.exceptions.MissingFolderOrFileException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class DatabaseMigrationServiceImpl implements DatabaseMigrationService {

//    private final String backupFolder = "data/";
    private final MediaTrackerDao mediaTrackerDao;
    private final XmlBackupService xmlBackupService;

    public DatabaseMigrationServiceImpl(
            @Qualifier("jpa") MediaTrackerDao mediaTrackerDao,
            XmlBackupService xmlBackupService,
            PropertiesService propertiesService) {
        this.mediaTrackerDao = mediaTrackerDao;
        this.xmlBackupService = xmlBackupService;
    }

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

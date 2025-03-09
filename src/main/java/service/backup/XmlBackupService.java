package service.backup;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import model.MediaLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import service.backup.model.BackupXml;
import service.exceptions.MissingFolderOrFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class XmlBackupService implements BackupService {

    private static final Logger LOG = LoggerFactory.getLogger(XmlBackupService.class);

    public XmlBackupService() {}


    String createBackupFileName() {
        DateTimeFormatter timeStamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "backup-" + timeStamp.format(LocalDateTime.now(ZoneId.systemDefault())) + ".xml";
    }

    private BackupXml prepareItemsForBackup(List<MediaLink> mediaLinks) {
        BackupXml backupXml = new BackupXml();
        backupXml.setMediaLinks(mediaLinks);
        return backupXml;
    }

    @Override
    public String exportRecords(Path dataDirectory, List<MediaLink> mediaLinkList)
            throws IOException, MissingFolderOrFileException {
        if (!checkAppDataFolder(dataDirectory)) {
            String error = "[ backup_service ] No data folder found: " + dataDirectory;
            LOG.error(error);
            throw new MissingFolderOrFileException(error);
        } else {
            XmlMapper xmlMapper = new XmlMapper();
            String backupFileName = createBackupFileName(); // prepare file name
            Path absoluteBackupPath = dataDirectory.resolve(backupFileName); // create full path
            String stringToWrite = xmlMapper.writeValueAsString(prepareItemsForBackup(mediaLinkList)); // prepare string
            Files.writeString(absoluteBackupPath, stringToWrite); // write to file
            return backupFileName;
        }
    }

    @Override
    public List<MediaLink> importRecords(Path fileNamePath)
            throws IOException, MissingFolderOrFileException {
        if (!checkAppDataFolder(fileNamePath)) {
            String error = "[ backup_service ] No backup file found: " + fileNamePath;
            LOG.error(error);
            throw new MissingFolderOrFileException(error);
        } else {
            String xmlAsString = Files.readString(fileNamePath);
            XmlMapper xmlMapper = new XmlMapper();
            return xmlMapper.readValue(xmlAsString, BackupXml.class).getMediaLinks();
        }
    }

    @Override
    public boolean checkAppDataFolder(Path dataFolder) {
        return Files.exists(dataFolder);
    }

}

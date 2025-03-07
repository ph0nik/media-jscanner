package service.backup;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import model.MediaLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.backup.model.BackupXml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class XmlBackupService implements BackupService {

    private static final Logger LOG = LoggerFactory.getLogger(XmlBackupService.class);
    private final Path dataFolder;

    public XmlBackupService(Path dataFolder) {
        this.dataFolder = dataFolder;
    }


    String getBackupFileName() {
        DateTimeFormatter timeStamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return "backup-" + timeStamp.format(LocalDateTime.now(ZoneId.systemDefault())) + ".xml";
    }
    @Override
    public BackupXml prepareItemsForBackup(List<MediaLink> mediaLinks) {
        BackupXml backupXml = new BackupXml();
        backupXml.setMediaLinks(mediaLinks);
        return backupXml;
    }

    @Override
    public String exportRecords(List<MediaLink> mediaLinkList) throws IOException {
        if (checkAppDataFolder()) {
            XmlMapper xmlMapper = new XmlMapper();
            String backupFileName = getBackupFileName();
            Path absoluteBackupPath = dataFolder.resolve(backupFileName);
            String stringToWrite = xmlMapper.writeValueAsString(prepareItemsForBackup(mediaLinkList));
            Files.writeString(absoluteBackupPath, stringToWrite);
//            xmlMapper.writeValue(absoluteBackupPath.toFile(), prepareItemsForBackup(mediaLinkList));
            return backupFileName;
        } else {
            LOG.error("[ backup_service ] Error: no data folder found");
        }
        return "";
    }

    @Override
    public List<MediaLink> importRecords(String fileName) throws IOException {
        Path absoluteBackupPath = dataFolder.resolve(fileName);
        String xmlAsString = Files.readString(absoluteBackupPath);
        XmlMapper xmlMapper = new XmlMapper();
        return xmlMapper.readValue(xmlAsString, BackupXml.class).getMediaLinks();
    }

    @Override
    public boolean checkAppDataFolder() {
        return Files.exists(dataFolder);
    }

}

package service.backup;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import model.MediaLink;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class XmlBackupServiceTest {

    private String dataFolder = "data";
    private BackupService backupService;
    private FileSystem fileSystem;
    private Path rootPath;

    @BeforeEach
    void prepareFileSystem() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.windows());
        rootPath = fileSystem.getPath("");
        Path testRootPath = rootPath.resolve(dataFolder);
        Files.createDirectory(testRootPath);
        prepareBackupService(testRootPath);
    }

    void prepareBackupService(Path dataPath) {
        backupService = new XmlBackupService(dataPath);
    }

    @Test
    @DisplayName("Serialize list with single element")
    void prepareAndBackupSingleItem() throws IOException {
        MediaLink ml = new MediaLink();
        ml.setImdbId("tt123435");
        ml.setLinkPath("link path");
        ml.setOriginalPath("original path");
        ml.setMediaId(1L);
        ml.setTheMovieDbId(12345);
        ml.setOriginalPresent(true);
        List<MediaLink> mediaLinkList = new ArrayList<>();
        mediaLinkList.add(ml);

        String exportPath = backupService.exportRecords(mediaLinkList);
        List<MediaLink> importedList = backupService.importRecords(exportPath);
        Assertions.assertEquals(mediaLinkList.size(), importedList.size());
        Random rnd = new Random();
        int index = rnd.nextInt(importedList.size());
        Assertions.assertEquals(importedList.get(index).getLinkPath(),
                mediaLinkList.get(index).getLinkPath());
        System.out.println(importedList);
    }

}
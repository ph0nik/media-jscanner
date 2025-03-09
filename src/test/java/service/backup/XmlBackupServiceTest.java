package service.backup;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import model.MediaLink;
import org.junit.jupiter.api.*;
import service.exceptions.MissingFolderOrFileException;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class XmlBackupServiceTest {

    private BackupService backupService;
    private Path testRootPath;
    private FileSystem fs;

    @BeforeEach
    void prepareFileSystem() throws IOException {
        Path rootPath;
        fs = Jimfs.newFileSystem(Configuration.windows());
        rootPath = fs.getPath("");
        String dataFolder = "data";
        testRootPath = rootPath.resolve(dataFolder);
        Files.createDirectory(testRootPath);
        backupService = new XmlBackupService();
    }

    @AfterEach
    void tearFileSystem() throws IOException {
        fs.close();
    }

    @Test
    @DisplayName("Serialize list with single element")
    void prepareAndBackupSingleItem() throws IOException, MissingFolderOrFileException {
        MediaLink ml = new MediaLink();
        ml.setImdbId("tt123435");
        ml.setLinkPath("link path");
        ml.setOriginalPath("original path");
        ml.setMediaId(1L);
        ml.setTheMovieDbId(12345);
        ml.setOriginalPresent(true);
        List<MediaLink> mediaLinkList = new ArrayList<>();
        mediaLinkList.add(ml);
        // export
        String exportFileName = backupService.exportRecords(testRootPath, mediaLinkList);
        // import
        List<MediaLink> importedList = backupService.importRecords(testRootPath.resolve(exportFileName));
        Assertions.assertEquals(mediaLinkList.size(), importedList.size());
        Random rnd = new Random();
        int index = rnd.nextInt(importedList.size());
        Assertions.assertEquals(
                importedList.get(index).getLinkPath(),
                mediaLinkList.get(index).getLinkPath()
        );
    }

}
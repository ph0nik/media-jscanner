package scanner;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import dao.MediaTrackerDao;
import org.junit.jupiter.api.*;
import service.PropertiesService;
import util.CleanerService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

class MediaFilesScannerTest {

    private PropertiesService propertiesService;
    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;
    private MediaFilesScanner mediaFilesScanner;
    private List<String> testFiles;
    private Path workPath;
    private FileSystem fileSystem;
    private String incomingFolder = "incoming";
    private String linkFolder = "complete";

    @BeforeEach
    void createFileTree() throws IOException {
        System.out.println("Creating file system...");
        Path hd = Paths.get("src/test/resources/relative_paths.txt");
        testFiles = Files.readAllLines(hd, StandardCharsets.ISO_8859_1);
        fileSystem = Jimfs.newFileSystem(Configuration.windows());
        Path next = fileSystem.getRootDirectories().iterator().next();
        workPath = next.resolve(incomingFolder);
//        rootPath = fileSystem.getPath("").resolve(incomingFolder);
        createFolderStructureWithFilesBasedOfListing();
    }

    @AfterEach
    void closeFileSystem() throws IOException {
        if (fileSystem.isOpen()) {
            fileSystem.close();
            System.out.println("File system is closed");
        } else {
            System.out.println("No file system found");
        }
    }


    @DisplayName("Create folders and files based of list from text file")
    void createFolderStructureWithFilesBasedOfListing() throws IOException {
        for (String path : testFiles) {
            Path of = Path.of(path);
            String file = of.getFileName().toString();
            Iterator<Path> iterator = of.getParent().iterator();
            Path dirPath = workPath;
            while (iterator.hasNext()) {
                dirPath = dirPath.resolve(iterator.next().toString());
            }
            Files.createDirectories(dirPath);
            Files.createFile(dirPath.resolve(file));
        }
    }

    @Test
    void checkIfCreatedFileIsPresent() {
        String testFile = "Filmy SD\\[Rip] Apocalypse Zero\\[RiP] Apocalypse Zero - 02 [C0C73373].mkv";
        System.out.println(workPath.getRoot().resolve(testFile));
        Assertions.assertTrue(Files.exists(workPath.resolve(testFile)));
    }


}


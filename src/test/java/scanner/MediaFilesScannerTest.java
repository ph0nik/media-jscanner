package scanner;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import dao.MediaTrackerDao;
import org.junit.jupiter.api.*;
import service.PropertiesService;
import util.CleanerService;
import util.MediaFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class MediaFilesScannerTest {

    private PropertiesService propertiesService;
    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;
    private MediaFilesScanner mediaFilesScanner;
    private List<String> testFiles;
    private Path workPath;
    private FileSystem fileSystem;
    private final String incomingFolder = "incoming";
    private final String linkFolder = "complete";

    @BeforeEach
    void createFileTree() throws IOException {
        System.out.println("Creating file system...");
        String moviePaths = "src/test/resources/relative_paths.txt";
        String seriesPaths = "src/test/resources/seriale_lista.txt";
        Path hd = Paths.get(seriesPaths);
        testFiles = Files.readAllLines(hd, StandardCharsets.ISO_8859_1);
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
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
        testFiles.stream()
                .filter(MediaFilter::validateExtension)
                .map(x -> workPath.resolve(x))
//                .peek(System.out::println)
                .forEach(this::createDirectoriesAndPath);
    }

    void createDirectoriesAndPath(Path p) {
        try {
            Files.createDirectories(p.getParent());
            Files.createFile(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void checkIfCreatedFileIsPresent() {
        String testFile = "Seriale/The.X-Files.S04.MULTi.1080p.Bluray.x265-BIBO/The.X-Files.S04E19.MULTi.1080p.Bluray.x265-BIBO.mkv";
        Assertions.assertTrue(Files.exists(workPath.resolve(testFile)));
    }


}


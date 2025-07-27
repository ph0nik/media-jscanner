package util;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

class CleanerServiceImplTest {

    private Path incomingPath;
    private Path linksPath;
    private String incomingFolder = "incoming";
    private String linkFolder = "complete";
    private Path filepath;
    private FileSystem fileSystem;
    private CleanerService cleanerService;


    @BeforeEach
    void createFileSystem() throws IOException {
        System.out.println("Creating file system...");

        fileSystem = Jimfs.newFileSystem(Configuration.windows());
        filepath = fileSystem.getPath("");
        incomingPath = filepath.resolve(incomingFolder);
        linksPath = filepath.resolve(linkFolder);
        cleanerService = new CleanerServiceImpl();
        createFolderStructureWithFilesBasedOfListing();
    }

    @AfterEach
    void closeFileSystem() throws IOException {
        if (fileSystem.isOpen()) fileSystem.close();
    }

    void createFolderStructureWithFilesBasedOfListing() throws IOException {
        Path hd = Paths.get("src/test/resources/relative_paths.txt");
        List<String> testFiles = Files.readAllLines(hd, StandardCharsets.ISO_8859_1);
        System.out.println(testFiles);
        for (String path : testFiles) {
            Path of = fileSystem.getPath(path);
//            Path of = Path.of(path);
            String file = of.getFileName().toString();
            Iterator<Path> iterator = of.getParent().iterator();
            Path dirPath = incomingPath;
            while (iterator.hasNext()) {
                dirPath = dirPath.resolve(iterator.next().toString());
            }
            Files.createDirectories(dirPath); // create folder chain for each file
            Files.createFile(dirPath.resolve(file)); // create file
        }
        Files.createDirectory(incomingPath.resolve(linkFolder)); // create destination folder for links
    }

    @Test
    @DisplayName("Creates a file system and file")
    void createSampleFile() throws IOException {
        String fileName = "newfile.txt";
        Path file = Files.createFile(filepath.resolve(fileName));
        Assertions.assertTrue(Files.exists(file));
    }

    @Test
    @DisplayName("Empty movie folder with video file inside")
    void deleteMovieFolderWithExtrasFolderAndMediaFileInside() {
        Path completePath = incomingPath.resolve("Filmy SD\\[Rip] Apocalypse Zero\\");
        cleanerService = new CleanerServiceImpl();
        cleanerService.clearEmptyFolders(completePath);
        Assertions.assertTrue(Files.exists(completePath));
    }

    @Test
    @DisplayName("Empty folder with no video files inside")
    void deleteMovieFolderWithoutAnyMediaFilesInside() {
        String movieFolder = "Filmy SD\\Haxxan (1922)\\";
        String subFile = "Haxan.1922.by.lutfucan.srt";
        Path completePath = incomingPath.resolve(movieFolder);
        // make sure file exists
        Assertions.assertTrue(Files.exists(completePath.resolve(subFile)));
        cleanerService.clearEmptyFolders(completePath);
        // check if file is deleted
        Assertions.assertFalse(Files.exists(completePath.resolve(subFile)));
    }

    @Test
    @DisplayName("Try to empty folder with non existing path")
    void deleteMovieFolder_wrongPath() {
        Path path = incomingPath.resolve("NonExistingFolder").resolve("Matrix");
        cleanerService.clearEmptyFolders(path);
    }

    @Test
    @DisplayName("Check if folder does not contain media files")
    void checkForMediaFiles_true() {
        Path path = incomingPath.resolve("NonExistingFolder").resolve("Matrix");
        boolean b = cleanerService.containsNoMediaFiles(path);
        Assertions.assertTrue(b);
    }

    @Test
    @DisplayName("Check media files in non existing folder")
    void checkForMediaFiles_malformedPath() {
        Path path = incomingPath.resolve("FilmySD/Matrix");
        boolean b = cleanerService.containsNoMediaFiles(path);
        Assertions.assertTrue(b);
    }


    @Test
    @DisplayName("Deleting single file")
    void deleteSingleFile() throws IOException {
        Path path = incomingPath.resolve("Filmy SD").resolve("[Rip] Apocalypse Zero");
        try (Stream<Path> list = Files.list(path)) {
            Path file = list.findFirst().orElse(Path.of(""));
            if (file.getNameCount() > 0) {
                cleanerService.deleteSingleFile(file);
            }
            Assertions.assertFalse(Files.exists(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
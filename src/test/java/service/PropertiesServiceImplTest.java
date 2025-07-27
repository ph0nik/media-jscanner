package service;

import app.config.EnvValidator;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import util.MediaType;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest(classes = {PropertiesService.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("dev")
class PropertiesServiceImplTest {

    private static PropertiesService propertiesService;
    private static FileSystem fileSystem;
    private static String dataFolder = "data";
    private static String dataFile = "mediafolders.properties";
    private static String incomingFolderMovies = "incomingMovies";
    private static String linksFolderMovies = "completeMovies";
    private static String otherIncomingFolderMovies = "otherIncomingMovies";
    private static String nonExistentFolder = "nonExistentFolder";
    private static Path otherIncomingMoviesPath;
    private static Path incomingMoviesPath;
    private static Path linksMoviesPath;
    private static Path dataPath;
    private static Path propertiesPath;

    @BeforeEach
    void init() {
        createFileSystem();
        propertiesService = new PropertiesServiceImpl(
                new EnvValidator(null),
                fileSystem);
    }

    static void createFileSystem() {
        fileSystem = Jimfs.newFileSystem(Configuration.windows());
        Path next = fileSystem.getRootDirectories().iterator().next();
        incomingMoviesPath = next.resolve(incomingFolderMovies);
        linksMoviesPath = next.resolve(linksFolderMovies);
        otherIncomingMoviesPath = next.resolve(otherIncomingFolderMovies);
//        dataPath = next.resolve(dataFolder);
//        propertiesPath = dataPath.resolve(dataFile);
    }

    @AfterAll
    void destroy() throws IOException {
        if (fileSystem.isOpen()) fileSystem.close();
    }

    @Test
    @DisplayName("Confirms that properties file exists")
    void checkIfPropertiesFileExist() {
        boolean exists = Files.exists(fileSystem.getPath(dataFolder, dataFile));
        Assertions.assertTrue(exists);
    }

    @Test
    @DisplayName("Confirms that movie paths are absent")
    void missingMoviePaths() {
        Assertions.assertFalse(propertiesService.areMoviePathsProvided());
    }


    @Test
    @DisplayName("Confirms that tv paths are absent")
    void missingTvPaths() {
        Assertions.assertFalse(propertiesService.areTvPathsProvided());
    }

    @Test
    @DisplayName("Confirms that movie source paths are present")
    void addSourceMoviePath() throws NoApiKeyException, ConfigurationException {
        propertiesService.addTargetPathMovie(incomingMoviesPath);
        Assertions.assertFalse(
                propertiesService
                        .getSourceFolderListMovie()
                        .contains(incomingMoviesPath)
        );
    }

    @Test
    @DisplayName("Confirms that movie link paths are present")
    void addLinksMoviePath() throws NoApiKeyException, ConfigurationException {
        propertiesService.addLinksPathMovie(linksMoviesPath);
        Assertions.assertEquals(linksMoviesPath, propertiesService.getLinksFolderMovie());
    }

    @Test
    @DisplayName("Adding and removing paths from list")
    void multipleSourceMovieFolders() throws NoApiKeyException, ConfigurationException {
        propertiesService
                .addTargetPathMovie(incomingMoviesPath)
                .addTargetPathMovie(otherIncomingMoviesPath);
        System.out.println(propertiesService.getSourceFolderListMovie());
        Assertions.assertEquals(2, propertiesService.getSourceFolderListMovie().size());
        propertiesService.removeTargetPathMovie(incomingMoviesPath);
        Assertions.assertEquals(1, propertiesService.getSourceFolderListMovie().size());
        Assertions.assertFalse(propertiesService.getSourceFolderListMovie().contains(incomingMoviesPath));
    }

    @Test
    @DisplayName("Check if non existent path is properly detected")
    void addingNonExistentFolder() throws NoApiKeyException, ConfigurationException, IOException {
        Path nonExistentPath = fileSystem.getRootDirectories().iterator().next().resolve(nonExistentFolder);
        propertiesService.addTargetPathMovie(nonExistentPath);
        propertiesService.addLinksPathMovie(linksMoviesPath);
        // check if paths are added
        Assertions.assertFalse(propertiesService.getSourceFolderListMovie().isEmpty());
        Assertions.assertFalse(propertiesService.getLinksFolderMovie().toString().isEmpty());
        // create one of paths in the filesystem
        Files.createDirectory(linksMoviesPath);
        // first path does not exist
        Assertions.assertFalse(propertiesService.doUserPathsExist(MediaType.MOVIE));
        Files.createDirectory(nonExistentPath);
        // now it does
        Assertions.assertTrue(propertiesService.doUserPathsExist(MediaType.MOVIE));
    }

}
package service;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import model.QueryResult;
import model.validator.RequiredFieldException;
import org.junit.jupiter.api.*;
import util.MediaIdentity;
import util.MediaType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//@Disabled
class FileServiceTest {

    private FileSystem fileSystem;
    private FileService fileService;
    private QueryResult queryResult;
    Path rootPath;
    String sourceFolder = "incoming";
    Path sourcePath;
    String linksFolder = "complete";
    Path linksPath;

    @BeforeEach
    void initFileService() {
        System.out.println("Creating file system...");
        fileSystem = Jimfs.newFileSystem(Configuration.windows());
        rootPath = fileSystem.getPath("R:\\media-jscanner-test");
        sourcePath = rootPath.resolve(sourceFolder);
        linksPath = rootPath.resolve(linksFolder);
        fileService = new FileService();
        setProperQueryResult();
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

    void setProperQueryResult() {
        queryResult = new QueryResult();
        queryResult.setId(0L);
        queryResult.setMediaType(MediaType.MOVIE);
        queryResult.setTitle("The Matrix");
        queryResult.setOriginalPath("R:\\media-jscanner-test\\incoming\\multipart_movie\\matrix-cd2.mkv");
        queryResult.setTheMovieDbId(603);
        queryResult.setImdbId("tt0133093");
        queryResult.setDescription("Action Sci-Fi When a beautiful stranger leads (...)");
        queryResult.setUrl("https://www.imdb.com/title/tt0133093/");
        queryResult.setQueryId(UUID.fromString("f1ae5651-0462-40b7-8f2a-762be79313d2"));
        queryResult.setPoster(null);
        queryResult.setYear("1999");
        queryResult.setMultipart((byte) 2);
    }

    @Test
    @DisplayName("Check path creation with correct parameters")
    void linkPathCreation_proper() throws FileNotFoundException, RequiredFieldException, IllegalAccessException {
        Path movieLinkPath = fileService.createMovieLinkPath_new(queryResult, MediaIdentity.IMDB, linksPath);
        Assertions.assertNotNull(movieLinkPath);
        Assertions.assertEquals(movieLinkPath.getFileName().toString(), "The Matrix-cd2.mkv");
    }

    @Test
    @DisplayName("Check possible null or empty fields of incoming objects")
    void linkPathCreation_nullCheck() throws FileNotFoundException, RequiredFieldException, IllegalAccessException {
        queryResult.setMediaType(null);
        Assertions.assertThrows(
                RequiredFieldException.class,
                () -> fileService.createMovieLinkPath(queryResult, MediaIdentity.IMDB, linksPath),
                "media type field should be null"
        );
        queryResult.setTitle(null);
        Assertions.assertThrowsExactly(
                RequiredFieldException.class,
                () -> fileService.createMovieLinkPath(queryResult, MediaIdentity.IMDB, linksPath),
                "title field should be null"
        );
        setProperQueryResult();
        queryResult.setYear(null);
        Assertions.assertThrowsExactly(
                RequiredFieldException.class,
                () -> fileService.createMovieLinkPath(queryResult, MediaIdentity.IMDB, linksPath),
                "year field should be null"
        );
        System.out.println("media identity null");
        setProperQueryResult();
        Assertions.assertNull(fileService.createMovieLinkPath(queryResult, null, linksPath));

        queryResult.setOriginalPath(null);
        Assertions.assertThrowsExactly(
                RequiredFieldException.class,
                () -> fileService.createMovieLinkPath(queryResult, MediaIdentity.IMDB, linksPath),
                "original path field should be null"
        );
    }

    @Disabled
    @Test
    void createFileStructureBasedOnList() throws IOException {
        String seriesPaths = "src/test/resources/seriale_lista.txt";
        Path path = Paths.get(seriesPaths);
        List<String> strings = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
        Path defaultFile = Path.of("r:\\");
        strings.stream()
                .map(s -> s.split(":")[0])
                .map(x -> Path.of("R:\\media-jscanner-test\\").resolve(x))
                .sorted(Comparator.reverseOrder())
//                .limit(1000)
//                .peek(System.out::println)
//                .forEach(this::createDirectoriesAndPath);
                .collect(Collectors.toList());
//        collect.forEach(this::createDirectoriesAndPath);

    }

    void createDirectoriesAndPath(Path p) {
        if (!Files.exists(p)) {
            int level = p.getNameCount();
            try {
                for (int i = 0; i < level; i++) {
                    Path resolve;
                    if (i == 0) {
                        resolve = p.getRoot().resolve(p.getName(0));
                    } else {
                        resolve = p.getRoot().resolve(p.subpath(0, i));
                    }
                    if (!Files.exists(resolve)) {
//                    System.out.println("// [D] " + resolve);
                        Files.createDirectory(resolve);
                    }
                }
//                System.out.println(">> [F} " + p);
                Files.createFile(p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
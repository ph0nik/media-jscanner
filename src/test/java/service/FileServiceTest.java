package service;

import model.QueryResult;
import org.junit.jupiter.api.*;
import util.MediaIdentity;
import util.MediaType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Disabled
class FileServiceTest {

    private FileService fileService;
    private QueryResult queryResult;
    Path defaultFile;

    @BeforeEach
    void initFileService() {
        fileService = new FileService();
        fileService.setLinksRootFolder(Path.of("m:\\"));
        setProperQueryResult();
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
    void linkPathCreation_proper() throws FileNotFoundException {
        Path movieLinkPath = fileService.createMovieLinkPath(queryResult, MediaIdentity.IMDB);
        Assertions.assertNotNull(movieLinkPath);
        Assertions.assertEquals(movieLinkPath.getFileName().toString(), "The Matrix-cd2.mkv");
    }

    @Test
    @DisplayName("Check possible null or empty fields of incoming objects")
    void linkPathCreation_nullCheck() throws FileNotFoundException {
        queryResult.setMediaType(null);
        Assertions.assertNotNull(fileService.createMovieLinkPath(queryResult, MediaIdentity.IMDB));
        queryResult.setTitle(null);
        Assertions.assertNull(fileService.createMovieLinkPath(queryResult, MediaIdentity.IMDB));
        setProperQueryResult();
        queryResult.setYear(null);
        Assertions.assertNull(fileService.createMovieLinkPath(queryResult, MediaIdentity.IMDB));
        setProperQueryResult();
        Assertions.assertNull(fileService.createMovieLinkPath(queryResult, null));
        queryResult.setOriginalPath(null);
        Assertions.assertNull(fileService.createMovieLinkPath(queryResult, MediaIdentity.IMDB));
    }

    @Disabled
    @Test
    void createFileStructureBasedOnList() throws IOException {
        String seriesPaths = "src/test/resources/seriale_lista.txt";
        Path path = Paths.get(seriesPaths);
        List<String> strings = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
        defaultFile = Path.of("r:\\");
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
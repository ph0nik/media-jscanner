package service;

import app.config.CacheConfig;
import app.config.EnvValidator;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import dao.MediaLinkRepository;
import dao.MediaTrackerDao;
import dao.MediaTrackerDaoJpa;
import model.MediaLink;
import model.MediaQuery;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import scanner.MediaFilesScanner;
import scanner.MoviesFileScanner;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import service.query.MediaQueryService;
import service.query.MovieQueryService;
import util.CleanerService;
import util.CleanerServiceImpl;
import util.TextExtractTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {CacheConfig.class})
@ActiveProfiles("dev")
class MediaLinksServiceImplTest {

    private MediaTrackerDao mediaTrackerDao;
    private PropertiesService propertiesService;
    private CleanerService cleanerService;
    private FileService fileService;
    private MediaQueryService mediaQueryService;
    private Pagination<MediaLink> linkPagination;
    private Pagination<MediaQuery> queryPagination;
    private MediaLinksServiceImpl mediaLinksService;
    private MediaFilesScanner mediaFilesScanner;
    private RequestService requestService;
    private ResponseParser responseParser;
    private DuplicateResolverServiceImpl duplicateResolverService;
    private List<String> testFiles;
    private Path incomingPath;
    private Path linksPath;
    private Path rootPath;
    private FileSystem fileSystem;
    private String incomingFolder = "incoming";
    private String linkFolder = "complete";
    private String testToken = "testToken";
    private EnvValidator envValidator;
    private MediaLinkRepository mediaLinkRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeAll
    void createInstances() throws NoApiKeyException, ConfigurationException {
        mediaFilesScanner = new MoviesFileScanner();
        mediaTrackerDao = new MediaTrackerDaoJpa(mediaLinkRepository);
        envValidator = new EnvValidator(null);
        propertiesService = new PropertiesServiceImpl(envValidator, fileSystem);
        cleanerService = new CleanerServiceImpl();
        fileService = new FileService();
        linkPagination = new PaginationImpl<>();
        queryPagination = new PaginationImpl<>();
        duplicateResolverService = new DuplicateResolverServiceImpl();
        requestService = new RequestService(propertiesService);
        responseParser = new ResponseParser(propertiesService);
        mediaQueryService = new MovieQueryService(mediaTrackerDao, mediaFilesScanner,
                propertiesService, queryPagination, cacheManager);
        mediaLinksService = new MediaLinksServiceImpl(mediaTrackerDao, propertiesService,
                cleanerService, fileService, linkPagination,
                requestService, responseParser, duplicateResolverService, cacheManager);
    }

    @BeforeEach
    void createFileTree() throws IOException {
        System.out.println("Creating file system...");
        Path hd = Paths.get("src/test/resources/relative_paths.txt");
        testFiles = Files.readAllLines(hd, StandardCharsets.ISO_8859_1);
        fileSystem = Jimfs.newFileSystem(Configuration.windows());
        rootPath = fileSystem.getPath("");
        incomingPath = rootPath.resolve(incomingFolder);
        linksPath = rootPath.resolve(linkFolder);
        createFolderStructureWithFilesBasedOfListing();

    }

    @AfterEach
    void closeFileSystem() throws IOException {
        if (fileSystem.isOpen()) {
            fileSystem.close();
        }
    }


    @DisplayName("Create folders and files based of list from text file")
    void createFolderStructureWithFilesBasedOfListing() throws IOException {
        for (String path : testFiles) {
            Path of = Path.of(path);
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
    @DisplayName("Extract group name from filename")
    void getGroupName() {
        URL resourceAsStream = getClass().getClassLoader().getResource("movies_hd.txt");
        File testList = new File(resourceAsStream.getPath());
        Map<String, String> output = new HashMap<>();
        try (Scanner sc = new Scanner(testList)) {
            while (sc.hasNextLine()) {
                String temp = sc.nextLine();
                String groupName = TextExtractTools.getGroupName(temp);
                if (!groupName.isEmpty()) {
                    output.put(temp, groupName);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Assertions.assertEquals(output.get("Demonic.2021.1080p.AMZN.WEB-DL.DDP5.1.H264-CMRG.mkv"), "CMRG");
    }

    @Test
    @DisplayName("Extract file descriptors")
    void checkForSpecialDescriptor_equals() throws IOException {
        Path hd = Paths.get("src/test/resources/movies_hd.txt");
        List<String> filesHd = Files.readAllLines(hd, StandardCharsets.ISO_8859_1);
        Map<String, String> output = filesHd.stream().collect(Collectors.toMap(f -> f, TextExtractTools::checkForSpecialDescriptor));
        Assertions.assertEquals(output.get("No.Estamos.Solos.2016.1080p.WEB-DL.AAC2.0.H.264-MooMa"), "1080p");
        Assertions.assertEquals(output.get("Klaus.2019.1080p.NF.WEB-DL.DDP5.1.HDR.HEVC-GLi.pl.srt"), "1080p hdr");
    }

    @Test
    @DisplayName("Create hard link based on existing file and check if process was successful")
    void createLink() throws IOException {
        MediaLink mediaLink = new MediaLink();
        mediaLink.setMediaId(1);
        mediaLink.setOriginalPresent(true);
        mediaLink.setImdbId("tt001122");
        Path absoluteLink = linksPath.resolve("Apocalypse Zero (" + mediaLink.getImdbId() + ")\\Apocalypse Zero-cd1.mkv");
        String linkPath = absoluteLink.toString();
        Path absoluteSource = incomingPath.resolve("Filmy SD\\[Rip] Apocalypse Zero\\[RiP] Apocalypse Zero - 02 [C0C73373].mkv");
        String originalPath = absoluteSource.toString();
        mediaLink.setLinkPath(linkPath);
        mediaLink.setOriginalPath(originalPath);
        mediaLink.setTheMovieDbId(12345);
        mediaLinksService.createHardLinkWithDirectories(absoluteLink, absoluteSource);
        Assertions.assertTrue(Files.exists(absoluteLink), "File not found.");
        Assertions.assertTrue(Files.isSameFile(absoluteLink, absoluteSource), "Different files");
    }

    @Test
    void testRootPath() {
        String linkPath = "/share/complete/movies/2017 Oscar Nominated Short Films_ Animation (2017) [imdbid-tt6511760]/2017 Oscar Nominated Short Films_ Animation - [1080p].mkv";
        System.out.println(fileSystem.getRootDirectories().iterator().next());
        Path of = Path.of(linkPath);
        System.out.println(of.getRoot().resolve(of.subpath(0, of.getNameCount() - 1)));
    }

}
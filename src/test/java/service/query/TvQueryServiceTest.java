package service.query;

import app.EnvValidator;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import model.MediaQuery;
import org.junit.jupiter.api.*;
import scanner.MediaFilesScanner;
import scanner.MoviesFileScanner;
import service.Pagination;
import service.PaginationImpl;
import service.PropertiesService;
import service.PropertiesServiceImpl;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import util.MediaFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TvQueryServiceTest {
    static MediaQueryService mediaQueryService;
    static MediaTrackerDao mediaTrackerDao;
    static MediaFilesScanner mediaFilesScanner;
    static PropertiesService propertiesService;
    static Pagination<MediaQuery> pagination;
    static List<MediaQuery> mediaQueryList;
    static List<String> testFiles;
    static FileSystem fileSystem;
    private static final String incomingFolder = "incoming";
    private static final String linkFolder = "complete";
    private static Path workPath;

    @BeforeAll
    void init() throws IOException, NoApiKeyException, ConfigurationException {
        createFileSystem();
        mediaTrackerDao = new MediaTrackerDaoImpl();
        mediaFilesScanner = new MoviesFileScanner();
        EnvValidator envValidator = new EnvValidator(null);
        propertiesService = new PropertiesServiceImpl(envValidator);
        propertiesService.addTargetPathTv(workPath.resolve("Seriale"));
        pagination = new PaginationImpl<>();
        mediaQueryService = new TvQueryService(mediaTrackerDao, mediaFilesScanner,
                propertiesService, pagination);
        getFileList();
//        createFolderStructureWithFilesBasedOfListing();
    }

    static void getFileList() throws IOException {
        String tvShows = "src/test/resources/seriale_lista.txt";
        Path movies = Paths.get(tvShows);
        testFiles = Files.readAllLines(movies, StandardCharsets.ISO_8859_1);
        mediaQueryList = testFiles.stream()
                .filter(MediaFilter::validateExtension)
                .map(f -> mediaQueryService.createMovieQuery(workPath.resolve(f)))
                .collect(Collectors.toList());
        mediaQueryService.setCurrentMediaQueries(mediaQueryList);
    }

    static void createFileSystem() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        Path next = fileSystem.getRootDirectories().iterator().next();
        workPath = next.resolve(incomingFolder);
    }

    @AfterAll
    static void deletePathFromConfig() throws NoApiKeyException, ConfigurationException {
        propertiesService.removeTargetPathTv(workPath.resolve("Seriale"));
    }

    @DisplayName("Create folders and files based of list from text file")
    static void createFolderStructureWithFilesBasedOfListing() {
        testFiles.stream()
                .map(x -> workPath.resolve(x))
                .forEach(TvQueryServiceTest::createDirectoriesAndPath);
    }

    static void createDirectoriesAndPath(Path p) {
        try {
            Files.createDirectories(p.getParent());
            Files.createFile(p);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    @Test
    void getGroupedTvShows() {
        mediaQueryService.groupByParentPathBatch(mediaQueryList);
//        int index = new Random().nextInt(mediaQueryList.size());
//        MediaQuery mediaQuery = mediaQueryList.get(index);
//        List<MediaQuery> groupedQueries = mediaQueryService.getGroupedQueries(mediaQuery.getQueryUuid());
//        groupedQueries.stream()
//                .map(MediaQuery::getFilePath)
//                .forEach(System.out::println);
    }

    @Test
    void differentAlgoForGrouping() {
        List<Path> mankind = mediaQueryList.stream()
                .map(MediaQuery::getFilePath)
                .map(Path::of)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        Map<Path, List<Path>> commonFolder = findCommonFolder(mankind, 2);
//        for (Path p : commonFolder.keySet()){
//            System.out.println("parent: " + p);
//            List<Path> paths = commonFolder.get(p);
//            System.out.println("elements: " + paths.size());
//            String s = paths.stream()
//                    .map(Path::toString)
//                    .findFirst()
//                    .orElse("");
//            System.out.println("\t" + s);
//        }


    }

    Map<Path, List<Path>> findCommonFolder(List<Path> path, int rootDepth) {
        Map<Path, List<Path>> groupedMap = new LinkedHashMap<>();
        if (path.isEmpty()) return groupedMap;
        Path candidateParent = path.get(0).getParent();
        // count episodes per parent
        // if parent dont match and number is 1 or greater than 2
        // then new season and new parent
        List<Path> currentFolder = new LinkedList<>();
        for (Path p : path) {
            int parentNameCount = candidateParent.getNameCount();
            while (parentNameCount > rootDepth) {
                candidateParent = candidateParent
                        .getRoot()
                        .resolve(candidateParent.subpath(0, parentNameCount--));
                if (p.startsWith(candidateParent)) {
                    currentFolder.add(p);
                    parentNameCount = -1;
                }
                if (currentFolder.size() > 1 && parentNameCount > 0) {
                    parentNameCount = rootDepth;
                }
            }
            if (parentNameCount == rootDepth) {
                groupedMap.put(candidateParent, currentFolder);
                currentFolder = new LinkedList<>();
                currentFolder.add(p);
                candidateParent = p.getParent();
            }
        }
        groupedMap.put(candidateParent, currentFolder);
        return groupedMap;
    }



}
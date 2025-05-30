package service;

import app.config.EnvValidator;
import model.form.SourcePathDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import util.MediaType;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Component
public class PropertiesServiceImpl implements PropertiesService {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesServiceImpl.class);
    private final String NETWORK_PROPERTIES_FILE = "network.properties";
    private final String USER_TARGET_MOVIE = "targetFolderMovie";
    private final String USER_LINKS_MOVIE = "linkFolderMovie";
    private final String USER_LINKS_TV = "linkFolderSeries";
    private final String USER_TARGET_TV = "targetFolderSeries";
    private final String API_KEY = "api_key_v4";
    // TODO let user input api key in config screen
    private Properties networkProperties;
    private Properties mediaFilesProperties;
    private FileSystem fileSystem;
    private String mediaFolders = "data_bak/mediafolders.properties";

    @Autowired
    private Environment env;

    @Value("${server.port}")
    private int serverPort;

    private final String tmdbApiToken;

    /*
     * TODO
     *  - create filesystem bean
     * - attach path to filesystem created
     * - get filesystem from test
     * */

    @Autowired
    public PropertiesServiceImpl(EnvValidator envValidator, FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        this.tmdbApiToken = envValidator.getTmdbApiToken();
        initUserDataPathsFile(fileSystem.getPath(mediaFolders));
//        initUserDataPathsFile(Path.of(mediaFolders));
        loadPropertiesFromFiles();
    }

    public PropertiesServiceImpl(EnvValidator envValidator, FileSystem fileSystem, String userDataFile) {
        this.tmdbApiToken = envValidator.getTmdbApiToken();
        this.fileSystem = fileSystem;
        this.mediaFolders = userDataFile;
        initUserDataPathsFile(fileSystem.getPath(userDataFile));
        loadPropertiesFromFiles();
    }

    public PropertiesServiceImpl(EnvValidator envValidator, Path userDataFile) {
        this.tmdbApiToken = envValidator.getTmdbApiToken();
        this.mediaFolders = userDataFile.toAbsolutePath().toString().concat(".test");
        initUserDataPathsFile(userDataFile);
        loadPropertiesFromFiles();
    }

    @PostConstruct
    void showAppPath() {
        LOG.info("\u001B[32m**** Application running at: http:\\\\localhost:{} ****\u001B[0m", serverPort);
    }

    public Path getDataFolder() {
//        return Path.of(mediaFolders).getParent();
        return fileSystem.getPath(mediaFolders).getParent();
    }

    void initUserDataPathsFile(Path filePath) {
        if (createDataFolder(filePath.getParent())) {
            createDataFile(filePath);
            createBackupFile(filePath);
        }
    }

    // TODO test all methods
    /*
     * Create a file if not exists
     * */
    void createDataFile(Path fileName) {
        if (!Files.exists(fileName)) {
            try {
                Files.createFile(fileName);
                LOG.info("[ props ] Media folders file created: {}", fileName);
            } catch (IOException e) {
                LOG.error("[ props ] Cannot create file: {}", e.getMessage());
            }
        } else {
            LOG.info("[ props ] Media folders file already exists: {}", fileName);
        }
    }

    void createBackupFile(Path fileName) {
        Path backup = fileName.getParent().resolve(Path.of(fileName.getFileName().toString().concat(".bak")));
        if (!Files.exists(backup)) {
            try {
                Files.createFile(backup);
                LOG.info("[ props ] Media folders backup file created: {}", backup);
            } catch (IOException e) {
                LOG.error("[ props ] Cannot create file: {}", e.getMessage());
            }
        } else {
            LOG.info("[ props ] Media folders backup file already exists: {}", backup);
        }
    }

    /*
     * Create data application folder if not exists
     * If folder is created properly or already exists, returns true
     * */
    boolean createDataFolder(Path dataFolder) {
        Path dirs = null;
        if (!Files.exists(dataFolder)) {
            try {
                dirs = Files.createDirectories(dataFolder);
            } catch (IOException e) {
                LOG.error("[ props ] Cannot create folder: {}", e.getMessage());
            }
            return dirs != null;
        }
        LOG.info("[ props ] Data folder already exists: {}", dataFolder);
        return true;
    }

    private void loadPropertiesFromFiles() {
        networkProperties = loadNetworkProperties();
        if (tmdbApiToken != null && !tmdbApiToken.isEmpty())
            networkProperties.setProperty(API_KEY, tmdbApiToken);
        mediaFilesProperties = loadExternalMediaFileProperties();
        validateAllProperties();
    }

    public Properties getNetworkProperties() {
        return networkProperties;
    }

    private void validateAllProperties() {
        LOG.info("[ props ] Validating network properties...");
        if (networkProperties == null || networkProperties.isEmpty()) {
            LOG.error("[ props ] No network properties found");
            return;
        }
        List<String> invalidProps = validateProperties(networkProperties);
        if (invalidProps.isEmpty()) {
            LOG.info("[ props ] Network properties are valid");
        } else {
            LOG.error("[ props ] Invalid network properties:");
            invalidProps.forEach(p -> {
                LOG.error("[ props ] Missing value for key '{}'", p);
            });
        }
        LOG.info("[ props ] Validating media folders properties...");
        if (mediaFilesProperties == null || mediaFilesProperties.isEmpty()) {
            LOG.error("[ props ] No media folders properties found");
            return;
        }
        invalidProps = validateProperties(mediaFilesProperties);
        if (invalidProps.isEmpty()) {
            LOG.info("[ props ] Media folders properties are valid");
        } else {
            LOG.error("[ props ] Invalid media folders properties:");
            invalidProps.forEach(p -> {
                LOG.error("[ props ] Missing value for key '{}'", p);
            });
        }
    }

    /*
     *  Check if properties file contains empty properties
     *  Return list of empty properties
     * */
    private List<String> validateProperties(Properties props) {
        Enumeration<?> networkKeys = props.propertyNames();
        List<String> output = new ArrayList<>();
        while (networkKeys.hasMoreElements()) {
            String key = (String) networkKeys.nextElement();
            String value = props.getProperty(key);
            if (value == null || value.isEmpty()) output.add(key);
        }
        return output;
    }

    /*
     * Check if user movie paths are present
     * */
    public boolean isMoviePathsProvided() {
        return !isFolderPropertyEmpty(mediaFilesProperties, USER_TARGET_MOVIE)
                && !isFolderPropertyEmpty(mediaFilesProperties, USER_LINKS_MOVIE);
    }

    /*
     * Check if user tv paths are present
     * */
    public boolean isTvPathsProvided() {
        return !isFolderPropertyEmpty(mediaFilesProperties, USER_TARGET_TV)
                && !isFolderPropertyEmpty(mediaFilesProperties, USER_LINKS_TV);
    }

    /*
     * Check if given properties file is empty or contain only white spaces
     * */
    private boolean isFolderPropertyEmpty(Properties props, String propertyKey) {
        String propertyValue = props.getProperty(propertyKey);
        if (propertyValue == null || propertyValue.isEmpty()) return true;
        return Arrays
                .stream(propertyValue.split(";"))
                .map(String::trim)
                .allMatch(String::isEmpty);
    }

    /*
     * Checks if provided user paths exist, takes MediaType as an argument and
     * returns true only if link folder is valid and at least one of source folders
     * exist.
     * */
    public boolean doUserPathsExist(MediaType mediaType) {
        if (mediaType == MediaType.MOVIE && isMoviePathsProvided()) {
            return getTargetFolderListMovie()
                    .stream()
                    .anyMatch(Files::exists)
                    && Files.exists(
                    Path.of(mediaFilesProperties.getProperty(USER_LINKS_MOVIE)
                    )
            );
        }
        if (mediaType == MediaType.TV && isTvPathsProvided()) {
            return getTargetFolderListTv()
                    .stream()
                    .anyMatch(Files::exists)
                    && Files.exists(
                    Path.of(mediaFilesProperties.getProperty(USER_LINKS_TV)
                    )
            );
        }
        return false;
    }

    public boolean areUserPathsProvided() {
        return isMoviePathsProvided() && isTvPathsProvided();
    }

    @Override
    public List<SourcePathDto> getSourcePathsDto(MediaType mediaType) {
        if (mediaType == MediaType.MOVIE) {
            return getTargetFolderListMovie()
                    .stream().map(SourcePathDto::new).toList();
        } else {
            return getTargetFolderListTv()
                    .stream().map(SourcePathDto::new).toList();
        }
    }

    /*
     * Get target folders with given properties keys
     * */
//    List<Path> loadTargetFolders(String userKey, String defaultKey) {
//        LOG.info("[ props ] Loading paths for: {}", userKey);
//        String targetFolder = mediaFilesProperties.getProperty(userKey);
//        if (isFolderPropertyEmpty(mediaFilesProperties, userKey)) {
//            LOG.error("[ props ] No user target paths found");
//            return List.of();
//        } else {
//            LOG.info("[ props ] User target paths found:");
//            return Arrays.stream(targetFolder.split(";"))
//                    .peek(p -> LOG.info("[ props ] \t {}", p))
//                    .map(Path::of)
//                    .collect(Collectors.toList());
//        }
//    }

    /*
     * Returns folder path for storing movie links.
     * */
    public Path getLinksFolderMovie() {
        String moviesLinksFolder = mediaFilesProperties.getProperty(USER_LINKS_MOVIE);
        return (moviesLinksFolder == null)
                ? Path.of("")
                : Path.of(moviesLinksFolder);
    }

    public Path getLinksFolderTv() {
        String seriesLinksFolder = mediaFilesProperties.getProperty(USER_LINKS_TV);
        return (seriesLinksFolder == null)
                ? Path.of("")
                : Path.of(seriesLinksFolder);
    }

    /*
     * Returns list of folders to be scanned.
     * */
    @Override
    public List<Path> getTargetFolderListMovie() {
        return getTypeListProperty(mediaFilesProperties, USER_TARGET_MOVIE);
    }

    @Override
    public List<Path> getTargetFolderListTv() {
        return getTypeListProperty(mediaFilesProperties, USER_TARGET_TV);
    }

    private List<Path> getTypeListProperty(Properties props, String key) {
        if (!props.containsKey(key) || props.getProperty(key).isEmpty()) return List.of();
        return Arrays.stream(
                        props
                                .getProperty(key)
                                .split(";")
                )
                .map(Path::of)
                .toList();
    }

    public PropertiesService addTargetPathTv(Path targetPath) {
        addTargetPath(targetPath, USER_TARGET_TV);
        return this;
    }

    /*
     * Add target folder path to path list.
     * */
    public PropertiesService addTargetPathMovie(Path targetPath) {
        addTargetPath(targetPath, USER_TARGET_MOVIE);
        return this;
    }

    private void addTargetPath(Path targetPath, String propertyKey) {
        if (targetPath != null && !targetPath.toString().isEmpty()) {
            StringBuilder targetPathString = new StringBuilder();
            if (isFolderPropertyEmpty(mediaFilesProperties, propertyKey)) {
                targetPathString.append(targetPath).append(";");
            } else {
                for (Path path : getTypeListProperty(mediaFilesProperties, propertyKey)) {
                    targetPathString.append(path).append(";");
                }
                targetPathString.append(targetPath).append(";");
            }

//        List<Path> typeListProperty = getTypeListProperty(mediaFilesProperties, propertyKey);
            mediaFilesProperties.setProperty(
                    propertyKey,
                    targetPathString.toString()
            );
            saveAndReload(mediaFilesProperties);
        }
    }

    @Override
    public void removeTargetPathMovie(Path targetPath) {
        removeTargetPath(targetPath, USER_TARGET_MOVIE);
    }

    @Override
    public void removeTargetPathTv(Path targetPath) {
        removeTargetPath(targetPath, USER_TARGET_TV);
    }

    public void removeTargetPath(Path targetPath, String targetPathKey) {
        if (targetPath != null && !targetPath.toString().isEmpty()) {
            List<Path> typeListProperty = getTypeListProperty(mediaFilesProperties, targetPathKey);
            mediaFilesProperties.setProperty(
                    targetPathKey,
                    typeListProperty
                            .stream()
                            .filter(p -> !p.equals(targetPath))
                            .map(Path::toString)
                            .reduce((a, b) -> a + ";" + b)
                            .orElse("")
            );
            LOG.info("[ props ] Removed source path: {}", targetPath);
            saveAndReload(mediaFilesProperties);
        }
    }

    /*
     * Set movie links path
     * */
    public void setLinksPathMovie(Path linksRoot) {
        mediaFilesProperties.setProperty(USER_LINKS_MOVIE, linksRoot.toString());
        saveAndReload(mediaFilesProperties);
    }

    /*
     * Set tv links path
     * */
    @Override
    public void setLinksPathTv(Path linksRoot) {
        mediaFilesProperties.setProperty(USER_LINKS_TV, linksRoot.toString());
        saveAndReload(mediaFilesProperties);
    }

    /*
     * Backup current properties file
     * Save current properties to file
     * */
    void saveAndReload(Properties props) {
        if (backupMediaPropertiesFile()) {
            try (final OutputStream outputStream = new FileOutputStream(mediaFolders)) {
                props.store(outputStream, "File updated");
            } catch (IOException e) {
                LOG.error("[ props ] Error saving folder properties: {}\n\t{}", mediaFolders, e.getMessage());
            }
        }
        loadPropertiesFromFiles();
    }

    /*
     * Backups media folder properties file each time it is updated
     * Returns true if successful false in case of error
     * */
    boolean backupMediaPropertiesFile() {
//        Path fileToMovePath = Paths.get(mediaFolders);
        Path fileToMovePath = fileSystem.getPath(mediaFolders);
        Path targetPath = fileSystem.getPath(mediaFolders + ".bak");
//        Path targetPath = Path.of(mediaFolders + ".bak");
        boolean deleted = false;
        try {
            deleted = Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            LOG.error("[ props ] Cannot delete backup file: {}\n\t{}", targetPath, e.getMessage());
        }
        Path backupFile = null;
        if (deleted) {
            LOG.info("[ props ] Media properties backup file deleted: {}", targetPath);
            try {
                backupFile = Files.copy(fileToMovePath, targetPath);
            } catch (IOException e) {
                LOG.error("[ props ] Cannot create backup file: {}\n\t{}", targetPath, e.getMessage());
            }
            if (backupFile != null) LOG.info("[ props ] Media properties backup file created: {}", targetPath);
        }
//        try {
//            deleted = Files.deleteIfExists(targetPath);
//            LOG.info("[ props ] Media properties backup file deleted: {}", targetPath);
//            backupFile = Files.copy(fileToMovePath, targetPath);
//            LOG.info("[ props ] Media properties backup file created: {}", targetPath);
//        } catch (IOException e) {
//            LOG.error("[ props ] Media properties file backup error: {}", e.getMessage());
//        }
        return deleted && backupFile != null;
    }

    /*
     * Loads media file properties from external file, with
     * user provided paths.
     * Returns non-empty properties object if no error occurred.
     * Returns empty properties object if there was problem with reading file.
     * */
    private Properties loadExternalMediaFileProperties() {
        Properties props = new Properties();
        File propertiesFile = new File(mediaFolders);
        try (final InputStream is = new FileInputStream(propertiesFile)) {
            props.load(is);
            LOG.info("[ props ] Loaded from external file.");
        } catch (FileNotFoundException e) {
            LOG.error("[ props ] No media folders file found: {}", mediaFolders);
        } catch (IOException e) {
            LOG.error("[ props ] Error reading file: {}", mediaFolders);
        }
        return props;
    }

    /*
     * Get properties object for network related properties.
     * */
    private Properties loadNetworkProperties() {
        Properties props = new Properties();
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(NETWORK_PROPERTIES_FILE)) {
            props.load(inputStream);
        } catch (IOException ioException) {
            LOG.error("[ props ] Error reading from input stream: {}", NETWORK_PROPERTIES_FILE);
        }
        return props;
    }
}

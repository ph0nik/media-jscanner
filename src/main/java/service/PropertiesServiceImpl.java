package service;

import app.config.EnvValidator;
import model.form.SourcePathDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import util.MediaType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PropertiesServiceImpl implements PropertiesService {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesServiceImpl.class);
    private final String MEDIA_FOLDERS_PROPERTIES_FILE = "mediafolders.properties";
    private final String EXTERNAL_MEDIA_FOLDER_PROPERTIES_FILE = "data/mediafolders.properties";
    private final String EXTERNAL_MEDIA_FOLDER_PROPERTIES_BAK = "data/mediafolders.properties.bak";
    private final String NETWORK_PROPERTIES_FILE = "network.properties";
    private final String USER_TARGET_MOVIE = "targetFolderMovie";
    private final String USER_LINKS_MOVIE = "linkFolderMovie";
    private final String USER_LINKS_TV = "linkFolderSeries";
    private final String USER_TARGET_TV = "targetFolderSeries";
    private final String API_KEY = "api_key_v4";
    private Properties networkProperties;
    private Properties mediaFilesProperties;

    private final String tmdbApiToken;

    public PropertiesServiceImpl(EnvValidator envValidator) {
        this.tmdbApiToken = envValidator.getTmdbApiToken();
        loadPropertiesFromFiles();
        createDataFolder();
    }

    public Path getDataFolder() {
        return Path.of(EXTERNAL_MEDIA_FOLDER_PROPERTIES_FILE).getParent();
    }

    /*
     * Create data application folder if not exists
     * */
    void createDataFolder() {
        File data = new File("data");
        if (!data.exists()) {
            try {
                Files.createDirectories(data.toPath());
            } catch (IOException e) {
                LOG.error("[ props ] Cannot create folder: {}", e.getMessage());
            }
        }
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
     * Check if user target path is provided
     * */
    public boolean userMoviePathsExist() {
        return !isPropertyEmpty(mediaFilesProperties, USER_TARGET_MOVIE)
                && !isPropertyEmpty(mediaFilesProperties, USER_LINKS_MOVIE);
    }

    /*
     * Check if user links path is provided
     * */
    public boolean userTvPathsExist() {
        return !isPropertyEmpty(mediaFilesProperties, USER_TARGET_TV)
                && !isPropertyEmpty(mediaFilesProperties, USER_LINKS_TV);
    }

    /*
     * Check if given properties file is empty or contain only white spaces
     * */
    private boolean isPropertyEmpty(Properties props, String propertyKey) {
        String propertyValue = props.getProperty(propertyKey);
        if (propertyValue == null) return true;
        else {
            return Arrays
                    .stream(propertyValue.split(";"))
                    .map(String::trim)
                    .allMatch(String::isEmpty);
        }
    }

    /*
     * Checks if provided user paths exist, takes MediaType as an argument and
     * returns true only if link folder is valid and at least one of source folders
     * exist.
     * */
    public boolean doUserPathsExist(MediaType mediaType) {
        if (mediaType == MediaType.MOVIE && userMoviePathsExist()) {
            return getTargetFolderListMovie()
                    .stream()
                    .anyMatch(Files::exists)
                    && Files.exists(
                    Path.of(mediaFilesProperties.getProperty(USER_LINKS_MOVIE))
            );
        }
        if (mediaType == MediaType.TV && userTvPathsExist()) {
            return getTargetFolderListTv()
                    .stream()
                    .anyMatch(Files::exists)
                    && Files.exists(
                    Path.of(mediaFilesProperties.getProperty(USER_LINKS_TV))
            );
        }
        return false;
    }

    public boolean userPathsPresent() {
        return userMoviePathsExist() && userTvPathsExist();
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
    List<Path> loadTargetFolders(String userKey, String defaultKey) {
        LOG.info("[ props ] Loading paths for: {}", userKey);
        String targetFolder = mediaFilesProperties.getProperty(userKey);
        if (isPropertyEmpty(mediaFilesProperties, userKey)) {
            LOG.error("[ props ] No user target paths found");
            return List.of();
        } else {
            LOG.info("[ props ] User target paths found:");
            return Arrays.stream(targetFolder.split(";"))
                    .peek(p -> LOG.info("[ props ] \t {}", p))
                    .map(Path::of)
                    .collect(Collectors.toList());
        }
    }

    /*
     * Returns folder path for storing movie links.
     * */
    public Path getLinksFolderMovie() {
        String moviesLinksFolder = mediaFilesProperties.getProperty(USER_LINKS_MOVIE);
        if (moviesLinksFolder == null) return Path.of("");
        return Path.of(moviesLinksFolder);
    }

    public Path getLinksFolderTv() {
        String seriesLinksFolder = mediaFilesProperties.getProperty(USER_LINKS_TV);
        if (seriesLinksFolder == null) return Path.of("");
        return Path.of(seriesLinksFolder);
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
        List<Path> typeListProperty = getTypeListProperty(mediaFilesProperties, propertyKey);
        StringBuilder targetPathString = new StringBuilder();
        for (Path path : typeListProperty) {
            targetPathString.append(path).append(";");
        }
        mediaFilesProperties.setProperty(
                propertyKey,
                targetPathString.append(targetPath.toString()).toString()
        );
        saveAndReload(mediaFilesProperties);
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
        saveAndReload(mediaFilesProperties);
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
        backupMediaPropertiesFile();
        try (final OutputStream outputStream = new FileOutputStream(EXTERNAL_MEDIA_FOLDER_PROPERTIES_FILE)) {
            props.store(outputStream, "File updated");
        } catch (IOException e) {
            LOG.error("[ props ] Error saving link path: {}", EXTERNAL_MEDIA_FOLDER_PROPERTIES_FILE);
        }
        loadPropertiesFromFiles();
    }

    /*
     * Backups media folder properties file each time it is updated
     * */
    void backupMediaPropertiesFile() {
        Path fileToMovePath = Paths.get(EXTERNAL_MEDIA_FOLDER_PROPERTIES_FILE);
        Path targetPath = Paths.get(EXTERNAL_MEDIA_FOLDER_PROPERTIES_BAK);
        try {
            Files.deleteIfExists(targetPath);
            LOG.info("[ props ] Media properties backup file deleted: {}", targetPath);
            Files.copy(fileToMovePath, targetPath);
            LOG.info("[ props ] Media properties backup file created: {}", targetPath);
        } catch (IOException e) {
            LOG.error("[ props ] Media properties file backup error: {}", e.getMessage());
        }
    }

    /*
     * Loads media file properties from external file, with
     * user provided paths.
     * Returns non-empty properties object if no error occurred.
     * Returns empty properties object if there was problem with reading file.
     * */
    private Properties loadExternalMediaFileProperties() {
        Properties props = new Properties();
        File propertiesFile = new File(EXTERNAL_MEDIA_FOLDER_PROPERTIES_FILE);
        try (final InputStream is = new FileInputStream(propertiesFile)) {
            props.load(is);
            LOG.info("[ props ] Loaded from external file.");
        } catch (FileNotFoundException e) {
            LOG.error("[ props ] No properties file found: {}", EXTERNAL_MEDIA_FOLDER_PROPERTIES_FILE);
        } catch (IOException e) {
            LOG.error("[ props ] Error reading file: {}", EXTERNAL_MEDIA_FOLDER_PROPERTIES_FILE);
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

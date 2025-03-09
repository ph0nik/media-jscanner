package service;

import app.EnvValidator;
import model.path.FilePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import util.MediaType;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PropertiesServiceImpl implements PropertiesService {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesServiceImpl.class);
    private final String MEDIA_FOLDERS_PROPERTIES_FILE = "mediafolders.properties";
    private final String EXTERNAL_MEDIA_FOLDERS_PROPERTIES_FILE = "data/mediafolders.properties";
    private final String NETWORK_PROPERTIES_FILE = "network.properties";
    private final String DEFAULT_TARGET_MOVIE = "defaultTargetFolderMovie";
    private final String USER_TARGET_MOVIE = "targetFolderMovie";
    private final String DEFAULT_LINKS_MOVIE = "defaultLinkFolderMovie";
    private final String USER_LINKS_MOVIE = "linkFolderMovie";
    private final String USER_LINKS_TV = "linkFolderSeries";
    private final String USER_TARGET_TV = "targetFolderSeries";
    private final String DEFAULT_TARGET_TV = "defaultTargetFolderShows";
    private final String DEFAULT_LINKS_TV = "defaultLinkFolderShows";
    private final String apiKeyPropertyKey = "api_key_v4";
    private Properties networkProperties;
    private Properties mediaFilesProperties;
    private Map<String, List<FilePath>> targetFolderMap;

    private String tmdbApiToken;

    public PropertiesServiceImpl(EnvValidator envValidator) {
        this.tmdbApiToken = envValidator.getTmdbApiToken();
        loadPropertiesFromFiles();
        createDataFolder();
    }

    @PostConstruct
    private void initProperies() {
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
            networkProperties.setProperty(apiKeyPropertyKey, tmdbApiToken);
        mediaFilesProperties = loadMediaFoldersProperties();
        loadFolderProperties();
        validateAllProperties();
    }

    public Properties getNetworkProperties() {
        return networkProperties;
    }

    private void validateAllProperties() {
        LOG.info("[ props ] Validating network properties...");
        List<String> props = validateProperties(networkProperties);
        if (props.isEmpty()) {
            LOG.info("[ props ] Network properties are valid");
        } else {
            LOG.error("[ props ] Invalid network properties:");
            props.forEach(p -> {
                LOG.error("[ props ] Missing value for key '{}'", p);
            });
        }
        LOG.info("[ props ] Validating media folders properties...");
        props = validateProperties(mediaFilesProperties);
        if (props.isEmpty()) {
            LOG.info("[ props ] Media folders properties are valid");
        } else {
            LOG.error("[ props ] Invalid media folders properties:");
            props.forEach(p -> {
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
     * Returns true if key value is not empty,
     * otherwise returns false.
     * */
    @Override
    public boolean checkApiToken() {
        return !networkProperties.getProperty(apiKeyPropertyKey).isEmpty();
    }

    private boolean checkPropertyValue(Properties props, String key) {
        return !props.getProperty(key).isEmpty();
    }

    /*
     * Check if user target path is provided
     * */
    public boolean isUserTargetPath() {
        return !isPropertyEmpty(mediaFilesProperties.getProperty(USER_TARGET_MOVIE))
                && !isPropertyEmpty(mediaFilesProperties.getProperty(USER_TARGET_TV));
    }

    /*
     * Check if user links path is provided
     * */
    public boolean isUserLinksPath() {
        return !isPropertyEmpty(mediaFilesProperties.getProperty(USER_LINKS_MOVIE))
                && !isPropertyEmpty(mediaFilesProperties.getProperty(USER_LINKS_TV));
    }

    /*
     * Check if given properties file is empty or contain only white spaces
     * */
    private boolean isPropertyEmpty(String propertyValue) {
        return Arrays.stream(propertyValue.split(";"))
                .map(String::trim)
                .allMatch(String::isEmpty);
    }

    public boolean checkUserPaths() {
        return isUserTargetPath() && isUserLinksPath();
    }

    void loadFolderProperties() {
        targetFolderMap = new HashMap<>();
        targetFolderMap.put(USER_TARGET_MOVIE,
                loadTargetFolders(USER_TARGET_MOVIE, DEFAULT_TARGET_MOVIE));
        targetFolderMap.put(USER_TARGET_TV,
                loadTargetFolders(USER_TARGET_TV, DEFAULT_TARGET_TV));
    }

    /*
     * Get target folders with given properties keys
     * */
    List<FilePath> loadTargetFolders(String userKey, String defaultKey) {
        LOG.info("[ props ] Loading paths for: {}", userKey);
        String targetFolder = mediaFilesProperties.getProperty(userKey);
        String paths;
        if (isPropertyEmpty(targetFolder)) {
            paths = (mediaFilesProperties.getProperty(defaultKey) != null)
                    ? mediaFilesProperties.getProperty(defaultKey)
                    : "";
            LOG.info("[ props ] No user target paths found, defaults loaded");
        } else {
            paths = targetFolder;
            LOG.info("[ props ] User target paths loaded");
        }
        LOG.info("[ props ] Target paths: {}", paths);
        return Arrays.stream(paths.split(";"))
                .map(p -> new FilePath(Path.of(p), true))
                .collect(Collectors.toList());
    }

    /*
     * Returns folder path for storing movie links.
     * */
    public Path getLinksFolderMovie() {
        return getLinksFolder(USER_LINKS_MOVIE, DEFAULT_LINKS_MOVIE);
    }

    public Path getLinksFolderTv() {
        return getLinksFolder(USER_LINKS_TV, DEFAULT_LINKS_TV);
    }

    /*
     * Read value for links folder from properties file.
     * Takes as parameters user and default keys.
     * Returns user defined path if present, otherwise returns default path.
     * */
    Path getLinksFolder(String userKey, String defaultKey) {
        String linkFolderPath = mediaFilesProperties.getProperty(userKey);
        Path links;
        if (isPropertyEmpty(linkFolderPath)) {
            links = Path.of(mediaFilesProperties.getProperty(defaultKey));
            LOG.info("[ props ] No user links path found, defaults loaded");
        } else {
            links = Path.of(linkFolderPath);
            LOG.info("[ props ] User links path loaded");
        }
        LOG.info("[ props ] Links path: {}", links);
        return links;
    }

    /*
     * Returns list of folders to be scanned.
     * */
    @Override
    public List<FilePath> getTargetFolderListMovie() {
        return targetFolderMap.get(USER_TARGET_MOVIE);
    }

    @Override
    public List<FilePath> getTargetFolderListTv() {
        return targetFolderMap.get(USER_TARGET_TV);
    }

    @Override
    public Boolean checkUserPath(Path path) {
        return path.toFile().exists();
    }

    // TODO read and write methods to any prop file with key value pair, with value as
    // single or list
    private void addTargetPath(Path targetPath, String propertyKey) {
        Properties props = loadMediaFoldersProperties();
        String property = props.getProperty(propertyKey);
        String[] currentPathsArray;
        String[] newPathsArray;
        if (!property.isEmpty()) {
            currentPathsArray = property.split(";");
            newPathsArray = new String[currentPathsArray.length + 1];
            System.arraycopy(currentPathsArray, 0, newPathsArray, 0, currentPathsArray.length);
            newPathsArray[newPathsArray.length - 1] = targetPath.toString();
        } else {
            newPathsArray = new String[]{targetPath.toString()};
        }
        props.setProperty(propertyKey, String.join(";", newPathsArray));
        saveAndReload(props);
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

    @Override
    public void removeTargetPathMovie(Path targetPath) {
        removeTargetPath(targetPath, MediaType.MOVIE);
    }

    @Override
    public void removeTargetPathTv(Path targetPath) {
        removeTargetPath(targetPath, MediaType.TV);
    }

    public void removeTargetPath(Path targetPath, MediaType mediaType) {
        Properties props = loadMediaFoldersProperties();
        String property = "";
        if (mediaType == MediaType.MOVIE) {
            property = USER_TARGET_MOVIE;
        }
        if (mediaType == MediaType.TV) {
            property = USER_TARGET_TV;
        }
        String[] split = props.getProperty(property).split(";");
        String out = Arrays.stream(split).filter(p -> !p.equals(targetPath.toString())).collect(Collectors.joining(";"));
        props.setProperty(property, out);
        saveAndReload(props);
    }

    /*
     * Set movie links path
     * */
    public void setLinksPathMovie(Path linksRoot) {
        Properties props = loadMediaFoldersProperties();
        props.setProperty(USER_LINKS_MOVIE, linksRoot.toString());
        saveAndReload(props);
    }

    /*
     * Set tv links path
     * */
    @Override
    public void setLinksPathTv(Path linksRoot) {
        Properties props = loadMediaFoldersProperties();
        props.setProperty(USER_LINKS_TV, linksRoot.toString());
        saveAndReload(props);

    }

    void saveAndReload(Properties props) {
        try (final OutputStream outputStream = new FileOutputStream(EXTERNAL_MEDIA_FOLDERS_PROPERTIES_FILE)) {
            props.store(outputStream, "File updated");
        } catch (IOException e) {
            LOG.error("[ props ] Error saving link path: {}", EXTERNAL_MEDIA_FOLDERS_PROPERTIES_FILE);
        }
        loadPropertiesFromFiles();
    }

    /*
     * Get properties object for symlink related properties.
     * First, method tries to obtain external properties file,
     * with user defined values. If that fails it loads
     * internal properties file with default values.
     * */
    private Properties loadMediaFoldersProperties() {
        Properties props = loadExternalMediaFileProperties();
        return (props.isEmpty()) ? loadInternalMediaFileProperties() : props;
    }

    /*
     * Loads media file properties from external file, with
     * user provided paths.
     * Returns non-empty properties object if no error occurred.
     * Returns empty properties object if there was problem with reading file.
     * */
    private Properties loadExternalMediaFileProperties() {
        Properties props = new Properties();
        File propertiesFile = new File(EXTERNAL_MEDIA_FOLDERS_PROPERTIES_FILE);
        try (final InputStream is = new FileInputStream(propertiesFile)) {
            props.load(is);
            LOG.info("[ props ] Loaded from external file.");
        } catch (FileNotFoundException e) {
            LOG.error("[ props ] No properties file found: {}", EXTERNAL_MEDIA_FOLDERS_PROPERTIES_FILE);
        } catch (IOException e) {
            LOG.error("[ props ] Error reading file: {}", EXTERNAL_MEDIA_FOLDERS_PROPERTIES_FILE);
        }
        return props;
    }

    /*
     * Loads media file properties from internal file,
     * with default values.
     * TODO remove default values start with empty paths
     * */
    private Properties loadInternalMediaFileProperties() {
        Properties props = new Properties();
        ClassLoader classLoader = getClass().getClassLoader();
        try (final InputStream is = classLoader.getResourceAsStream(MEDIA_FOLDERS_PROPERTIES_FILE)) {
            props.load(is);
            LOG.info("[ props ] No external file found, loading default values");
        } catch (IOException e) {
            LOG.error("[ props ] Error reading file: {}", MEDIA_FOLDERS_PROPERTIES_FILE);
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

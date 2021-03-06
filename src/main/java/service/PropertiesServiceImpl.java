package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class PropertiesServiceImpl implements PropertiesService {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesServiceImpl.class);

    //    private static final String configPath = "src/main/resources/mediafolders.properties";
    private static final String MEDIA_FOLDERS_PROPERTIES_FILE = "mediafolders.properties";
    private static final String NETWORK_PROPERTIES_FILE = "network.properties";
    private static final String DEFAULT_TARGET_PATH = "defaultTargetFolderMovie";
    private static final String USER_TARGET_PATH = "targetFolderMovie";
    private static final String DEFAULT_LINKS_PATH = "defaultLinkFolderMovie";
    private static final String USER_LINKS_PATH = "linkFolderMovie";

    private Properties networkProperties;
    private Properties mediaFilesProperties;

    public PropertiesServiceImpl() {
        loadPropertiesFromFiles();
    }

    private void loadPropertiesFromFiles() {
        networkProperties = loadNetworkProperties();
        mediaFilesProperties = loadMediaFoldersProperties();
    }

    public Properties getNetworkProperties() {
        return networkProperties;
    }

    /*
    * Check if user target path is provided
    * */
    public boolean isUserTargetPath() {
        String property = mediaFilesProperties.getProperty(USER_TARGET_PATH);
        return !isPropertyEmpty(property);
    }

    /*
    * Check if user links path is provided
    * */
    public boolean isUserLinksPath() {
        String property = mediaFilesProperties.getProperty(USER_LINKS_PATH);
        return !isPropertyEmpty(property);
    }

    /*
    * Check if given properties file is empty or contain only white spaces
    * */
    private boolean isPropertyEmpty(String propertyValue) {
        String[] split = propertyValue.split(";");
        return Arrays.stream(split)
                    .map(String::trim)
                    .allMatch(String::isEmpty);
    }

    public boolean checkUserPaths() {
        return isUserTargetPath() && isUserLinksPath();
    }

    /*
    * Returns list of folders to be watched.
    * */
    public List<Path> getTargetFolderList() {
        String paths;
        String targetFolderMovie = mediaFilesProperties.getProperty(USER_TARGET_PATH);
        if (isPropertyEmpty(targetFolderMovie)) {
            LOG.info("[ props ] No user target paths found, defaults loaded");
            paths = mediaFilesProperties.getProperty(DEFAULT_TARGET_PATH);
        } else {
            LOG.info("[ props ] User target paths loaded");
            paths = targetFolderMovie;
        }
        return Arrays.stream(paths.split(";")).map(Path::of).collect(Collectors.toList());
    }

    /*
    * Returns folder where symlinks should be stored.
    * */
    public Path getLinksFolder() {
        String linkFolderPath = mediaFilesProperties.getProperty(USER_LINKS_PATH);
        if (isPropertyEmpty(linkFolderPath)) {
            LOG.info("[ props ] No user links path found, defaults loaded");
            return Path.of(mediaFilesProperties.getProperty(DEFAULT_LINKS_PATH));
        } else {
            LOG.info("[ props ] User links path loaded");
            return Path.of(linkFolderPath);
        }
    }

    /*
     * Add target folder path to path list.
     * */
    public void setTargetPath(Path targetPath) {
        Properties props = loadMediaFoldersProperties();
        String property = props.getProperty(USER_TARGET_PATH);
        StringBuilder sb = new StringBuilder("");
        String[] pathList = property.split(";");
        for (String path : pathList) {
            if (path.length() > 0) sb.append(path).append(";");
        }
        sb.append(targetPath.toString());
        props.setProperty(USER_TARGET_PATH, sb.toString());
        try (final OutputStream outputStream = new FileOutputStream(MEDIA_FOLDERS_PROPERTIES_FILE)) {
            props.store(outputStream, "File updated");
            LOG.info("[ props ] Target path added: {}", targetPath);
        } catch (IOException e) {
            LOG.error("[ props ] Error saving target path: {}", MEDIA_FOLDERS_PROPERTIES_FILE);
        }
        loadPropertiesFromFiles();
    }

    /*
     * Remove target path from property file.
     * */
    public void removeTargetPath(Path targetPath) {
        Properties props = loadMediaFoldersProperties();
        String property = props.getProperty(USER_TARGET_PATH);
        String[] split = property.split(";");
        StringBuilder sb = new StringBuilder("");
        for (String s : split) {
            if (!s.equals(targetPath.toString()) && !s.isEmpty()) sb.append(s).append(";");
        }
        props.setProperty(USER_TARGET_PATH, sb.toString());
        try (final OutputStream outputStream = new FileOutputStream(MEDIA_FOLDERS_PROPERTIES_FILE)) {
            props.store(outputStream, "File updated");
            LOG.info("[ props ] Target path removed: {}", targetPath);
        } catch (IOException e) {
            LOG.error("[ props ] {}", e.getMessage());
        }
        loadPropertiesFromFiles();
    }

    /*
     * Set links path
     * */
    public void setLinksPath(Path linksRoot) {
        Properties props = loadMediaFoldersProperties();
        props.setProperty(USER_LINKS_PATH, linksRoot.toString());
        try (final OutputStream outputStream = new FileOutputStream(MEDIA_FOLDERS_PROPERTIES_FILE)) {
            props.store(outputStream, "File updated");
        } catch (IOException e) {
            LOG.error("[ props ] Error saving link path: {}", MEDIA_FOLDERS_PROPERTIES_FILE);
        }
        loadPropertiesFromFiles();
    }

    /*
     * Get properties object for symlink related properties.
     * First, method tries to obtain external properties file,
     * with user defined values. If that fails it loads
     * internal properties file, with default values.
     * */
    private Properties loadMediaFoldersProperties() {
        Properties props = loadExternalMediaFileProperties();
        if (props.isEmpty()) props = loadInternalMediaFileProperties();
        return props;
    }

    /*
    * Loads media file properties from external file, with
    * user provided paths.
    * Returns non-empty properties object if no error occurred.
    * Returns empty properties object if there was problem with reading file.
    * */
    private Properties loadExternalMediaFileProperties() {
        Properties props = new Properties();
        File propertiesFile = new File(MEDIA_FOLDERS_PROPERTIES_FILE);
        try (final InputStream is = new FileInputStream(propertiesFile)) {
            props.load(is);
            LOG.info("[ props ] Loaded from external file.");
        } catch (FileNotFoundException e) {
            LOG.error("[ props ] No properties file found: {}", MEDIA_FOLDERS_PROPERTIES_FILE);
        } catch (IOException e) {
            LOG.error("[ props ] Error reading file: {}", MEDIA_FOLDERS_PROPERTIES_FILE);
        }
        return props;
    }

    /*
    * Loads media file properties from internal file,
    * with default values.
    * */
    private Properties loadInternalMediaFileProperties() {
        Properties props = new Properties();
        ClassLoader classLoader = getClass().getClassLoader();
        try (final InputStream is = classLoader.getResourceAsStream(MEDIA_FOLDERS_PROPERTIES_FILE)){
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

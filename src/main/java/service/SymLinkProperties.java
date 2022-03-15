package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class SymLinkProperties {

    private static final Logger LOG = LoggerFactory.getLogger(SymLinkProperties.class);

    //    private static final String configPath = "src/main/resources/mediafolders.properties";
    private static final String MEDIA_FOLDERS_PROPERTIES_FILE = "mediafolders.properties";
    private static final String NETWORK_PROPERTIES_FILE = "network.properties";
    private static final String DEFAULT_TARGET_PATH = "defaultTargetFolderMovie";
    private static final String USER_TARGET_PATH = "targetFolderMovie";
    private static final String DEFAULT_LINKS_PATH = "defaultLinkFolderMovie";
    private static final String USER_LINKS_PATH = "linkFolderMovie";

    private Properties mediaFoldersProperties;
    private Properties networkProperties;
    private boolean userTargetPath;
    private boolean userLinksPath;

    public SymLinkProperties() {
        loadProperties();
    }

    // load properties at object creation
    private void loadProperties() {
        mediaFoldersProperties = loadMediaFoldersProperties();
        networkProperties = loadNetworkProperties();
    }

    // reload properties from files
    public void reloadProperties() {
        loadProperties();
    }

    public Properties getMediaFoldersProperties() {
        return mediaFoldersProperties;
    }

    public Properties getNetworkProperties() {
        return networkProperties;
    }

    public boolean isUserTargetPath() {
        return userTargetPath;
    }

    public boolean isUserLinksPath() {
        return userLinksPath;
    }

    public boolean checkUserPaths() {
        return userTargetPath && userLinksPath;
    }

    public List<Path> getTargetFolderList() {
        String targetFolderMovie = mediaFoldersProperties.getProperty(USER_TARGET_PATH);
        String defaultTargetFolderMovie = mediaFoldersProperties.getProperty(DEFAULT_TARGET_PATH);
        String paths;
        if (targetFolderMovie == null || targetFolderMovie.isEmpty()) {
            LOG.info("[ props ] Target path is empty");
            paths = defaultTargetFolderMovie;
            userTargetPath = false;
        } else {
            LOG.info("[ props ] Target path found");
            paths = targetFolderMovie;
            userTargetPath = true;
        }
//        String paths = (targetFolderMovie == null || targetFolderMovie.isEmpty())
//                ? defaultTargetFolderMovie
//                : targetFolderMovie;
        String[] split = paths.split(";");
        // TODO stream
//        List<Path> pathStream = Arrays.stream(split).map(Path::of).collect(Collectors.toList());
        List<Path> output = new ArrayList<>();
        for (String s : split) {
            output.add(Path.of(s));
        }
        return output;
    }

    public Path getLinksFolder() {
        String linkFolderMovie = mediaFoldersProperties.getProperty(USER_LINKS_PATH);
        String defaultLinkFolderMovie = mediaFoldersProperties.getProperty(DEFAULT_LINKS_PATH);
        if (linkFolderMovie == null || linkFolderMovie.isEmpty()) {
            userLinksPath = false;
            return Path.of(defaultLinkFolderMovie);
        }
        userLinksPath = true;
        return Path.of(linkFolderMovie);
    }

    /*
     * Set target path
     * */
    public Properties setTargetPath(Path targetPath) {
        Properties props = mediaFoldersProperties;
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
        } catch (IOException e) {
            LOG.error("[ props ] {}", e.getMessage());
        }
        return props;
    }

    /*
    * Remove target path from property file.
    * */
    public Properties removeTargetPath(Path targetPath) {
        Properties props = mediaFoldersProperties;
        String property = props.getProperty(USER_TARGET_PATH);
        String[] split = property.split(";");
        StringBuilder sb = new StringBuilder("");
        for (String s : split) {
            if (!s.equals(targetPath.toString()) && !s.isEmpty()) sb.append(s).append(";");
        }
        props.setProperty(USER_TARGET_PATH, sb.toString());
        try (final OutputStream outputStream = new FileOutputStream(MEDIA_FOLDERS_PROPERTIES_FILE)) {
            props.store(outputStream, "File updated");
        } catch (IOException e) {
            LOG.error("[ props ] {}", e.getMessage());
        }
        return props;
    }

    /*
     * Set links path
     * */
    public Properties setLinksPath(Path linksRoot) {
        Properties props = mediaFoldersProperties;
        props.setProperty(USER_LINKS_PATH, linksRoot.toString());
        try (final OutputStream outputStream = new FileOutputStream(MEDIA_FOLDERS_PROPERTIES_FILE)) {
            props.store(outputStream, "File updated");
            outputStream.close();
        } catch (IOException e) {
            LOG.error("[ props ] {}", e.getMessage());
        }
        return props;
    }

    /*
     * Get properties object for symlink related properties.
     *
     * */
    private Properties loadMediaFoldersProperties() {
        Properties props = new Properties();
        File externalProperties = new File(MEDIA_FOLDERS_PROPERTIES_FILE);
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(externalProperties);
            LOG.info("[ props ] Loaded from external file.");
        } catch (FileNotFoundException e) {
            LOG.error("[ props ] {}", e.getMessage());
            LOG.info("[ props ] Loaded default values");
            ClassLoader classLoader = getClass().getClassLoader();
            inputStream = classLoader.getResourceAsStream(MEDIA_FOLDERS_PROPERTIES_FILE);
        }
        if (inputStream == null) {
            // TODO ????
            throw new IllegalStateException("[ props ] File '" + MEDIA_FOLDERS_PROPERTIES_FILE + "' not found! ");
        } else {
            try {
                props.load(inputStream);
            } catch (IOException e) {
                LOG.error("[ props ] {}", e.getMessage());
            }
        }
        return props;
    }

    /*
     * Get properties object for network related properties.
     * */
    private Properties loadNetworkProperties() {
        Properties props = new Properties();
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(NETWORK_PROPERTIES_FILE);
        if (inputStream == null) {
            throw new IllegalStateException("[ props ] File '" + NETWORK_PROPERTIES_FILE + "' not found! ");
        } else {
            try {
                props.load(inputStream);
            } catch (IOException e) {
                LOG.error("[ props ] {}", e.getMessage());
            }
        }
        return props;
    }

}

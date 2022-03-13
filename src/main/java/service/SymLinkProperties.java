package service;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class SymLinkProperties {

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
            paths = defaultTargetFolderMovie;
            userTargetPath = false;
        } else {
            paths = targetFolderMovie;
            userTargetPath = true;
        }
//        String paths = (targetFolderMovie == null || targetFolderMovie.isEmpty())
//                ? defaultTargetFolderMovie
//                : targetFolderMovie;
        String[] split = paths.split(";");
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
            e.printStackTrace();
        }
        return props;
    }

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
            e.printStackTrace();
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
            e.printStackTrace();
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
//            userPathsProvided = true;
            System.out.println("[ properties ] loaded from external file.");
        } catch (FileNotFoundException e) {
            System.out.println("[ properties ] default");
            ClassLoader classLoader = getClass().getClassLoader();
            inputStream = classLoader.getResourceAsStream(MEDIA_FOLDERS_PROPERTIES_FILE);
//            userPathsProvided = false;
        }
        if (inputStream == null) {
            throw new IllegalStateException("[ properties ] File not found! " + MEDIA_FOLDERS_PROPERTIES_FILE);
        } else {
            try {
                props.load(inputStream);
            } catch (IOException e) {
                System.out.println("[ properties ] " + e.getMessage());
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
            throw new IllegalStateException("[ properties ] File not found! " + NETWORK_PROPERTIES_FILE);
        } else {
            try {
                props.load(inputStream);
            } catch (IOException e) {
                System.out.println("[ properties ] " + e.getMessage());
            }
        }
        return props;
    }

}

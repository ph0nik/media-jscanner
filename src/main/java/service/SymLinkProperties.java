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

    private Properties mediaFoldersProperties;
    private Properties networkProperties;

    public SymLinkProperties() {
        loadProperties();
    }

    private void loadProperties() {
        mediaFoldersProperties = getSymLinkProperties();
        networkProperties = getNetworkProperties();
    }

    public Properties getMediaFoldersProperties() {
        return mediaFoldersProperties;
    }


    public List<Path> getTargetFolderList() {
        String targetFolderMovie = mediaFoldersProperties.getProperty("targetFolderMovie");
        String defaultTargetFolderMovie = mediaFoldersProperties.getProperty("defaultTargetFolderMovie");
        String paths = (targetFolderMovie == null || targetFolderMovie.isEmpty())
                ? defaultTargetFolderMovie
                : targetFolderMovie;
        String[] split = paths.split(";");
        List<Path> output = new ArrayList<>();
        for (String s : split) {
            output.add(Path.of(s));
        }
        return output;
    }

    public Path getLinksFolder() {
        String linkFolderMovie = mediaFoldersProperties.getProperty("linkFolderMovie");
        String defaultLinkFolderMovie = mediaFoldersProperties.getProperty("defaultLinkFolderMovie");
        if (linkFolderMovie == null || linkFolderMovie.isEmpty()) return Path.of(defaultLinkFolderMovie);
        return Path.of(linkFolderMovie);
    }

    /*
     * Set target path
     * */
    public Properties setTargetPath(Path targetPath) {
        Properties props = mediaFoldersProperties;
        String targetFolder = "targetFolderMovie";
        String property = props.getProperty(targetFolder);
        String s = "";
        if (property == null) s = targetPath.toString();
        else {
            s = new StringBuilder(property)
                    .append(";")
                    .append(targetPath.toString())
                    .toString();
        }
        props.setProperty("targetFolderMovie", s);
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
        props.setProperty("linkFolderMovie", linksRoot.toString());
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
    public Properties getSymLinkProperties() {
        Properties props = new Properties();
        File externalProperties = new File(MEDIA_FOLDERS_PROPERTIES_FILE);
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(externalProperties);
            System.out.println("[ properties ] loaded from external file.");
        } catch (FileNotFoundException e) {
            System.out.println("[ properties ] default");
            ClassLoader classLoader = getClass().getClassLoader();
            inputStream = classLoader.getResourceAsStream(MEDIA_FOLDERS_PROPERTIES_FILE);
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
    public Properties getNetworkProperties() {
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

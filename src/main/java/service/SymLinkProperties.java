package service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class SymLinkProperties {

    private static final String configPath = "src/main/resources/mediafolders.properties";
    private static final String MEDIA_FOLDERS_PROPERTIES_FILE = "mediafolders.properties";
    private static final String NETWORK_PROPERTIES_FILE = "network.properties";
    private static Path movieFolder;
    private static Path showsFolder;

    // TODO cleanup
    public static void loadSymLinkProperties() {
        try (
                InputStream is = new FileInputStream(configPath)) {
                Properties symLinkProperties = new Properties();
                symLinkProperties.load(is);
                movieFolder = Path.of(symLinkProperties.getProperty("linkFolderMovie"));
                showsFolder = Path.of(symLinkProperties.getProperty("linkFolderSeries"));
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    public Properties getFoldersFromExternalFile(List<Path> folders) {
        Properties props = new Properties();
        ClassLoader classLoader = getClass().getClassLoader();
        String s = folders.get(0).toString();
        InputStream resourceAsStream = classLoader.getResourceAsStream(s);
        if (resourceAsStream == null) {
            throw new IllegalStateException("[ properties ] File not found! " + s);
        } else {
            try {
                props.load(resourceAsStream);
            } catch (IOException e) {
                System.out.println("[ properties ] " + e.getMessage());
            }
        }
        return props;
    }

    public Properties getSymLinkProperties() {
        Properties props = new Properties();
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(MEDIA_FOLDERS_PROPERTIES_FILE);
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

    public static Path getMovieFolder() {
        return movieFolder;
    }

    public static Path getShowsFolder() {
        return showsFolder;
    }

}

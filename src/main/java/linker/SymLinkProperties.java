package linker;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public class SymLinkProperties {

    private static final String configPath = "src/main/resources/mediafolders.properties";
    private static Path movieFolder;
    private static Path showsFolder;

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

    public static Path getMovieFolder() {
        return movieFolder;
    }

    public static Path getShowsFolder() {
        return showsFolder;
    }

}

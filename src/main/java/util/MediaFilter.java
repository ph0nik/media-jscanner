package util;

import java.nio.file.Path;
import java.util.List;

public class MediaFilter {

    private static final List<String> extensions = List.of(".mkv", ".avi", ".rmvb", ".wmv", ".mpg",
            ".mpeg", ".mpv", ".ts", ".ogm", ".ogv", ".m2v", ".qt", ".mov", ".asf", ".mp4", ".m4v", ".m2ts", ".vob", ".vp6", ".av1", ".vc1", ".flv");

    /*
     * Checking if given element name ends with one of given extensions.
     * If any of listed elements match given string method returns true.
     * */
    public static boolean validateExtension(String name) {
        for (String s : extensions) {
            if (name.endsWith(s)) return true;
        }
        return false;
    }

    public static List<String> getExtensions() {
        return List.copyOf(extensions);
    }

    public static boolean validateExtension(Path path) {
        return extensions.stream()
                .anyMatch(path.toString()::endsWith);
    }

    /*
     * Returns file name without extension
     * */
    public static String getFileName(String file) {
        return file.substring(0, file.lastIndexOf("."));
    }

}

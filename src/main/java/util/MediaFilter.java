package util;

import java.io.File;
import java.util.List;

public class MediaFilter {

    private static final List<String> extensions = List.of(".mkv", ".avi", ".rmvb", ".wmv", ".mpg",
            ".mpeg", ".mpv", ".ogm", ".m2v", ".qt", ".mov", ".asf", ".mp4", ".m4v");
    //'avi', 'mkv', 'rmvb', 'wmv', 'mpg', 'mpeg', 'mpv', 'ogm', 'm2v', 'qt', 'mov', 'asf', 'mp4', 'm4v'

    /*
    * Checking if given element name ends with one of given extensions.
    * */
    public static boolean validateExtension(String name) {
        for (String s : extensions) {
            if (name.endsWith(s)) return true;
        }
        return false;
    }

    /*
    * Iterates over elements in provided directory, if none of the existing elements
    * matches given extensions directory is treated as empty and returns true, otherwise returns false.
    * */
    public static boolean checkForEmptyDirectory(File file) {
        File[] fileList = file.listFiles();
        if (fileList != null) {
            for (File fileName : fileList) {
                for (String ext : extensions) {
                    if (fileName.getName().endsWith(ext)) return false;
                }
            }
        }
        return true;
    }

    /*
    * Returns file name without extension
    * */
    public static String getFileName(String file) {
        return file.substring(0, file.lastIndexOf("."));
    }
}

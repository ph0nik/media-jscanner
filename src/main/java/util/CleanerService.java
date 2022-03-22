package util;

import dao.MediaTrackerDao;

import java.nio.file.Path;

public interface CleanerService {

    /*
    * Check if folder with given path contains elements of type (extension)
    * defined by user as part of collection.
    * If none of such elements are found, folder is considered as empty
    * and method returns true.
    * */
    boolean containsNoMediaFiles(Path linkPath);

    /*
    * Deletes folder and contained elements with given path.
    * */
    void deleteElement(Path linkPath);

    /*
    * Using file walk tree, searches for empty folders and deletes them.
    * */
    void deleteEmptyFolders(Path root);

    /*
    * Using links data from database, deletes all unregistered symlinks.
    * */
    void deleteInvalidLinks(Path root, MediaTrackerDao dao);

}

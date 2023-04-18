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
    void deleteElements(Path linkPath);

    boolean deleteSingleFile(Path path);

    /*
    * Deletes empty directories.
    * Directory is considered empty if it contains no directory and no media files.
    * */
    void clearEmptyFolders(Path root);

    /*
    * Deletes all database entries with invalid paths
    * */
    int deleteInvalidDbEntries(MediaTrackerDao mediaTrackerDao);

    // TODO add function to clear original file
    // clear all links if target file is in specified folder
    // revert changes by creating hard link based of media link object property

    // TODO function to clear all non existent files from db

    // TODO get link by name (search box)

    /*
    * Checks if any media query elements point to non-existing files.
    * Every invalid records is deleted.
    * params:   queries - list of media query elements
    *           dao - dao service
    * */
//    void deleteInvalidMediaQuery(List<MediaQuery> queries, MediaTrackerDao dao);


//    void deleteInvalidIgnoredMedia(MediaTrackerDao mediaTrackerDao);

}

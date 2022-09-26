package util;

import dao.MediaTrackerDao;
import model.MediaIgnored;
import model.MediaLink;
import model.MediaQuery;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface CleanerService {

    /*
    * Check if folder with given path contains elements of type (extension)
    * defined by user as part of collection.
    * If none of such elements are found, folder is considered as empty
    * and method returns true.
    * */
    boolean containsNoMediaFiles(Path linkPath) throws IOException;

    /*
    * Deletes folder and contained elements with given path.
    * */
    void deleteElement(Path linkPath) throws IOException;

    /*
    * Checks if parent folder of given file contains any elements matching criteria.
    * If not, method removes all the content and parent folder.
    * */
    void clearParentFolder(Path file) throws IOException;

    /*
    * Using file walk tree, searches for empty folders and deletes them.
    * */
    void deleteEmptyFolders(Path root);

    /*
    * Checks if any media query elements point to non-existing files.
    * Every invalid records is deleted.
    * params:   queries - list of media query elements
    *           dao - dao service
    * */
    void deleteInvalidMediaQuery(List<MediaQuery> queries, MediaTrackerDao dao);


    void deleteInvalidLink(List<MediaLink> links, MediaTrackerDao dao);

    void deleteInvalidIgnoredMedia(List<MediaIgnored> mediaIgnoredList, MediaTrackerDao dao);

    /*
    * Using links data from database, deletes all unregistered symlinks.
    * */
    void deleteInvalidLinks(Path root, MediaTrackerDao dao) throws IOException;

}

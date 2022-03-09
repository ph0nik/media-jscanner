package util;

import java.nio.file.Path;

public interface CleanerService {

    /*
    * Check if folder with given path contains elements of type (extension)
    * defined by user as part of collection.
    * If none of such elements are found, folder is considered as empty
    * and method returns true.
    * */
    public boolean containsNoMediaFiles(Path linkPath);

    /*
    * Delete folder with given path
    * */
    void deleteElement(Path linkPath);

    /*
    * Delete all elements that don't match user criteria.
    * */
    public void deleteNonMediaFiles(Path path);


    public void cleanUpLinkFolder(Path root);


}

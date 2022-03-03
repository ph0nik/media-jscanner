package util;

public interface CleanerService {

    /*
    * Check if folder with given path contains elements of type (extension)
    * defined by user as part of collection.
    * If none of such elements are found, folder is considered as empty
    * and method returns true.
    * */
    public boolean isFolderEmpty(String linkPath);

    /*
    * Delete folder with given path
    * */
    public void deleteFolder(String linkPath);

}

package service;

import model.MediaLink;
import model.MediaQuery;

public interface MediaFilesService {

    /*
    * Adds new query, based on found file path to the queue.
    * */
    void addQuery(String filePath);

    /*
    * Removes specified symbolic link
    * */
    void removeLink(MediaLink mediaLink);

    /*
    * Removes specified query from queue
    * */
    void removeQuery(MediaQuery mediaQuery);

    /*
    * Removes link based on file path
    * */
    void removeLinkByFilePath(String filePath);

    /*
    * Removes query based on target file path
    * */
    void removeQueryByFilePath(String filePath);
}

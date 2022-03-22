package service;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public interface PropertiesService {


    public Properties getNetworkProperties();

    public boolean isUserTargetPath();

    public boolean isUserLinksPath();

    public boolean checkUserPaths();

    /*
     * Returns list of folders to be watched.
     * */
    public List<Path> getTargetFolderList();

    /*
     * Returns folder where symlinks should be stored.
     * */
    public Path getLinksFolder();

    /*
     * Add target folder path to path list.
     * */
    public void setTargetPath(Path targetPath);


    /*
     * Remove target path from property file.
     * */
    public void removeTargetPath(Path targetPath);


    /*
     * Set links path
     * */
    public void setLinksPath(Path linksRoot);



}

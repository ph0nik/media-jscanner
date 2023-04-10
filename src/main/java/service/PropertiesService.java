package service;

import model.path.FilePath;

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
    public List<FilePath> getTargetFolderListMovie();

    /*
     * Returns folder where symlinks should be stored.
     * */
    public Path getLinksFolderMovie();

    List<FilePath> getTargetFolderListTv();

    /*
     * Add target folder path to path list.
     * */
    public void addTargetPathMovie(Path targetPath);

    public void addTargetPathTv(Path targetPath);

    /*
     * Set links path
     * */
    public void setLinksPath(Path linksRoot);


    void removeTargetPathMovie(Path of);

    void removeTargetPathTv(Path of);

    Path getLinksFolderTv();
}

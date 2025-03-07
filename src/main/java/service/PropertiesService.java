package service;

import model.path.FilePath;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public interface PropertiesService {


    public Properties getNetworkProperties();

//    public Properties getApiToken(Properties networkProperties);

    boolean checkApiToken();

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
    public PropertiesService addTargetPathMovie(Path targetPath) throws NoApiKeyException, ConfigurationException;

    public PropertiesService addTargetPathTv(Path targetPath) throws NoApiKeyException, ConfigurationException;

    /*
     * Set links path
     * */
    public void setLinksPathMovie(Path linksRoot) throws NoApiKeyException, ConfigurationException;


    void removeTargetPathMovie(Path of) throws NoApiKeyException, ConfigurationException;

    void removeTargetPathTv(Path of) throws NoApiKeyException, ConfigurationException;

    Path getLinksFolderTv();

    /*
    * Set tv links path
    * */
    void setLinksPathTv(Path linksRoot) throws NoApiKeyException, ConfigurationException;
}

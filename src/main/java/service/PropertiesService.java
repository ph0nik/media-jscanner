package service;

import model.form.SourcePathDto;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import util.MediaType;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public interface PropertiesService {

    public Path getDataFolder();
    public Properties getNetworkProperties();

    public boolean userMoviePathsExist();

    public boolean userTvPathsExist();

    public boolean userPathsPresent();

    public boolean doUserPathsExist(MediaType mediaType);
    /*
     * Returns list of folders to be watched.
     * */
    public List<Path> getTargetFolderListMovie();

    List<SourcePathDto> getSourcePathsDto(MediaType mediaType);

    /*
     * Returns folder where symlinks should be stored.
     * */
    public Path getLinksFolderMovie();
    List<Path> getTargetFolderListTv();
    /*
     * Add target folder path to path list.
     * */
    public PropertiesService addTargetPathMovie(Path targetPath) throws NoApiKeyException, ConfigurationException;

    public PropertiesService addTargetPathTv(Path targetPath) throws NoApiKeyException, ConfigurationException;

    /*
     * Set links path
     * */
    public void setLinksPathMovie(Path linksRoot) throws NoApiKeyException, ConfigurationException;


    void removeTargetPathMovie(Path of) throws ConfigurationException;

    void removeTargetPathTv(Path of) throws ConfigurationException;

    Path getLinksFolderTv();

    /*
    * Set tv links path
    * */
    void setLinksPathTv(Path linksRoot) throws NoApiKeyException, ConfigurationException;
}

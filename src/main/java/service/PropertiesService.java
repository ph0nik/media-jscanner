package service;

import model.form.SourcePathDto;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import util.MediaType;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public interface PropertiesService {

    String getApiKey();

    String getApiKeyPartial();

    public Path getDataFolder();
    public Properties getNetworkProperties();

    public boolean areMoviePathsProvided();

    public boolean areTvPathsProvided();

//    public boolean areUserPathsProvided();

    public boolean doUserPathsExist(MediaType mediaType);
    /*
     * Returns list of folders to be watched.
     * */
    public List<Path> getSourceFolderListMovie();

    List<SourcePathDto> getSourcePathsDto(MediaType mediaType);

    /*
     * Returns folder where symlinks should be stored.
     * */
    public Path getLinksFolderMovie();
    List<Path> getSourceFolderListTv();
    /*
     * Add target folder path to path list.
     * */
    public PropertiesService addTargetPathMovie(Path targetPath) throws NoApiKeyException, ConfigurationException;

    public PropertiesService addTargetPathTv(Path targetPath) throws NoApiKeyException, ConfigurationException;

    /*
     * Set links path
     * */
    public void addLinksPathMovie(Path linksRoot) throws NoApiKeyException, ConfigurationException;


    void removeTargetPathMovie(Path of) throws ConfigurationException;

    void removeTargetPathTv(Path of) throws ConfigurationException;

    Path getLinksFolderTv();

    /*
    * Set tv links path
    * */
    void addLinksPathTv(Path linksRoot) throws NoApiKeyException, ConfigurationException;
}

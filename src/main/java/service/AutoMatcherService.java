package service;

import model.MediaLink;
import service.exceptions.NetworkException;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;

public interface AutoMatcherService {

    // take movie file name
    // find four numbers and extract phrase before them
    // use phrase as search query with numbers as year
    // perform tmdb search and get results
    // if list contains only one element apply this single result

    Future<List<MediaLink>> autoMatchFilesWithFuture() throws NetworkException;

    Future<List<MediaLink>> autoMatchAndGetLinks() throws NetworkException;

    List<MediaLink> autoMatchSingleFile(Path path) throws NetworkException;

}

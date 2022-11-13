package service;

import dao.MediaTrackerDao;
import model.MediaQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import scanner.MediaFilesScanner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MediaQueryService {

//    private final MediaFilesScanner mediaFilesScanner;
    private List<MediaQuery> mediaQueriesList = new LinkedList<>();

    // TODO move all connections to dao into here
    @Autowired
    @Qualifier("spring")
    private MediaTrackerDao mediaTrackerDao;
    @Autowired
    private MediaFilesScanner mediaFilesScanner;

//    @Autowired
//    private CleanerService cleanerService;

//    public MediaQueryService(@Qualifier("spring") MediaTrackerDao mediaTrackerDao, CleanerService cleanerService) {
//        this.mediaFilesScanner = new MediaFilesScanner(mediaTrackerDao, cleanerService);
//        mediaQueriesList = List.of();
//    }

    // scan given paths and gather all files matching criteria
    // except ones that are already ignored or already has links
    public void scanForNewMediaQueries(List<Path> paths) throws IOException {
//        mediaQueriesList = mediaFilesScanner.scanMediaFolders(paths);
        List<Path> candidates = mediaFilesScanner.scanMediaFolders(paths, mediaTrackerDao.getAllMediaLinks());
        mediaQueriesList = new LinkedList<>();
        for (Path c : candidates) {
            addQueryToQueue(c.toString());
        }
    }

    public MediaQuery addQueryToQueue(String filepath) {
        MediaQuery mq = new MediaQuery();
        mq.setQueryUuid(UUID.randomUUID());
        mq.setFilePath(filepath);
        mediaQueriesList.add(mq);
        return mq;
    }

    public List<MediaQuery> searchQuery(String search) {
        return mediaQueriesList.stream()
                .filter(mq -> mq.getFilePath().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
    }

    /*
    * Returns current media query list
    * */
    public List<MediaQuery> getCurrentMediaQueries() {
        return mediaQueriesList;
    }

    /*
    * Returns query with given id
    * */
    public MediaQuery getQueryById(Long id) {
        Optional<MediaQuery> first = mediaQueriesList
                .stream()
                .filter(x -> x.getQueryId() == id)
                .findFirst();
        return first.orElse(null);
    }

    /*
    * Returns query with given file path
    * */
    public MediaQuery findQueryByFilePath(String filepath) {
        Optional<MediaQuery> first = mediaQueriesList
                .stream()
                .filter(x -> x.getFilePath().equals(filepath))
                .findFirst();
        return first.orElse(null);
    }

    /*
    * Removes given element from the query list
    * */
    public void removeQueryFromQueue(MediaQuery mediaQuery) {
        mediaQueriesList = getCurrentMediaQueries()
                .stream()
                .filter(x -> x.getQueryUuid() != mediaQuery.getQueryUuid())
                .collect(Collectors.toList());
    }

    /*
    * Removes list element with given path
    * */
    public void removeQueryByFilePath(String path) {
        mediaQueriesList = getCurrentMediaQueries()
                .stream()
                .filter(x -> !x.getFilePath().equals(path))
                .collect(Collectors.toList());
    }

    public MediaQuery getQueryByUuid(String uuid) {
        Optional<MediaQuery> first = mediaQueriesList
                .stream()
                .filter(x -> x.getQueryUuid().toString().equals(uuid))
                .findFirst();
        return first.orElse(null);
    }
}

package service;

import dao.MediaTrackerDao;
import model.MediaQuery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import scanner.MediaFilesScanner;
import util.CleanerService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MediaQueryService {

    private final MediaFilesScanner mediaFilesScanner;
    private List<MediaQuery> mediaQueriesList;

    public MediaQueryService(@Qualifier("spring") MediaTrackerDao mediaTrackerDao, CleanerService cleanerService) {
        this.mediaFilesScanner = new MediaFilesScanner(mediaTrackerDao, cleanerService);
        mediaQueriesList = List.of();
    }

    // scan given paths and gather all files matching criteria
    // except ones that are already ignored or already has links
    public void scanForNewMediaQueries(List<Path> paths) throws IOException {
        mediaQueriesList = mediaFilesScanner.scanMediaFolders(paths);
    }

    public MediaQuery addQueryToQueue(String filepath) {
        MediaQuery mq = new MediaQuery();
        mq.setQueryUuid(UUID.randomUUID());
        mq.setFilePath(filepath);
        mediaQueriesList.add(mq);
        return mq;
    }

    // get current list from memory without scanning
    public List<MediaQuery> getCurrentMediaQueries() {
        return mediaQueriesList;
    }

    public MediaQuery getQueryById(Long id) {
        Optional<MediaQuery> first = mediaQueriesList
                .stream()
                .filter(x -> x.getQueryId() == id)
                .findFirst();
        return first.orElse(null);
    }

    public MediaQuery findQueryByFilePath(String filepath) {
        Optional<MediaQuery> first = mediaQueriesList
                .stream()
                .filter(x -> x.getFilePath().equals(filepath))
                .findFirst();
        return first.orElse(null);
    }

    public void removeQueryFromQueue(MediaQuery mediaQuery) {
        mediaQueriesList = getCurrentMediaQueries()
                .stream()
                .filter(x -> x.getQueryUuid() != mediaQuery.getQueryUuid())
                .collect(Collectors.toList());
    }

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

package service;

import dao.MediaTrackerDao;
import model.MediaQuery;
import model.multipart.MultiPartElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import scanner.MediaFilesScanner;
import util.MediaType;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MediaQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaQueryService.class);

    private List<MediaQuery> mediaQueriesList = new LinkedList<>();

    private List<MediaQuery> groupedQueriesToProcess;

    private MediaQuery referenceQuery;
    private Map<Path, List<UUID>> mediaQueriesByRootMap = new HashMap<>();

    @Autowired
    @Qualifier("spring")
    private MediaTrackerDao mediaTrackerDao;
    @Autowired
    private MediaFilesScanner mediaFilesScanner;

    public MediaQuery getReferenceQuery() {
        return referenceQuery;
    }

    public void setReferenceQuery(UUID mediaQueryUuid) {
        referenceQuery = getQueryByUuid(mediaQueryUuid);
    }

    // scan given paths and gather all files matching criteria
    // except ones that are already ignored or already has links
    public void scanForNewMediaQueries(List<Path> paths) {
        List<Path> candidates = mediaFilesScanner.scanMediaFolders(paths, mediaTrackerDao.getAllMediaLinks());
        mediaQueriesList = new LinkedList<>();
//        mediaQueriesByRootMap = new HashMap<>();
        candidates.forEach(c -> addQueryToQueue(c.toString()));
    }

    public MediaQuery addQueryToQueue(String filepath) {
        MediaQuery mq = new MediaQuery(filepath);
        mediaQueriesList.add(mq);
        groupByParentPathBatch(mediaQueriesList);
        return mq;
    }

    /*
     * Removes given element from the query list
     * */
    public void removeQueryFromQueue(MediaQuery mediaQuery) {
        mediaQueriesList = getCurrentMediaQueries()
                .stream()
                .filter(mq -> !mq.getQueryUuid().equals(mediaQuery.getQueryUuid()))
                .collect(Collectors.toList());
        groupByParentPathBatch(mediaQueriesList);
    }

    /*
     * Removes list element with given path
     * */
    public void removeQueryByFilePath(String path) {
        mediaQueriesList = getCurrentMediaQueries()
                .stream()
                .filter(mq -> !mq.getFilePath().equals(path))
                .collect(Collectors.toList());
        groupByParentPathBatch(mediaQueriesList);
    }

    public MediaQuery getQueryByUuid(UUID uuid) {
        Optional<MediaQuery> first = mediaQueriesList
                .stream()
                .filter(x -> x.getQueryUuid().equals(uuid))
                .findFirst();
        return first.orElse(null);
    }

    void groupByParentPathBatch(List<MediaQuery> mediaQueryList) {
        mediaQueriesByRootMap = new HashMap<>();
        mediaQueryList.forEach(this::groupByParentPath);
    }

    /*
     * Group media query element ids by parent folder
     * */
    void groupByParentPath(MediaQuery mediaQuery) {
        Path parent = Path.of(mediaQuery.getFilePath()).getParent();
        List<UUID> uuids = (mediaQueriesByRootMap.get(parent) == null) ? new LinkedList<>() : mediaQueriesByRootMap.get(parent);
        uuids.add(mediaQuery.getQueryUuid());
        mediaQueriesByRootMap.put(parent, uuids);
    }

    /*
     * Returns list of media queries of elements sharing the same folder at the same file tree level.
     * */
    public List<MediaQuery> getGroupedQueries(UUID mediaQueryUuid) {
        if (mediaQueriesByRootMap.isEmpty()) return List.of();
        Path parent = Path.of(getQueryByUuid(mediaQueryUuid).getFilePath()).getParent();
        return mediaQueriesByRootMap.get(parent)
                .stream()
                .map(this::getQueryByUuid)
                .peek(System.out::println)
                // after creating link other files within the same folder are ignored, so they won't appear here
                .filter(query -> query.getMultipart() == -1)
                .collect(Collectors.toList());
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

    public List<MediaQuery> getProcessList() {
        return List.copyOf(groupedQueriesToProcess);
    }

    /*
    * Adds single query to process list, it's called when new link is created
    * */
    public List<MediaQuery> addQueryToProcess(MediaQuery mediaQuery) {
        if (mediaQuery.getMediaType() == null) mediaQuery.setMediaType(MediaType.MOVIE);
        groupedQueriesToProcess = List.of(mediaQuery);
        return List.copyOf(groupedQueriesToProcess);
    }

    public List<MediaQuery> addQueriesToProcess(List<MultiPartElement> mediaQueryList) {
        groupedQueriesToProcess = new LinkedList<>();
        int count = 0;
        for (MediaQuery current : mediaQueriesList) {
            for (MultiPartElement mpe : mediaQueryList) {
                if (mpe.getFilePath().equals(current.getFilePath()) && mpe.getMultipartSwitch() != 0) {
                    current.setMultipart(mpe.getPartNumber());
                    current.setMediaType(mpe.getMediaType());
                    groupedQueriesToProcess.add(current);
                    count++;
                }
            }
        }
        LOG.info("[ query_service ] Grouped {} elements", count);
        // if none selected add reference query to queue
        if (count == 0) {
            LOG.info("[ query_service ] No queries marked, adding reference to the queue");
            addQueryToProcess(referenceQuery);
        }
        return List.copyOf(groupedQueriesToProcess);
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

}
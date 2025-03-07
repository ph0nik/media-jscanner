package service.query;

import dao.MediaTrackerDao;
import model.MediaQuery;
import model.path.FilePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import scanner.MediaFilesScanner;
import service.Pagination;
import service.PropertiesService;
import util.MediaType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component("movieQuery")
public class MovieQueryService extends GeneralQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(MovieQueryService.class);
    private final MediaTrackerDao mediaTrackerDao;
    private final MediaFilesScanner moviesFileScanner;
    private final PropertiesService propertiesService;
    private Map<Path, List<UUID>> mediaQueriesByRootMap;

    public MovieQueryService(@Qualifier("spring") MediaTrackerDao mediaTrackerDao,
                             MediaFilesScanner moviesFileScanner,
                             PropertiesService propertiesService,
                             Pagination<MediaQuery> pagination) {
        super(pagination);
        this.mediaTrackerDao = mediaTrackerDao;
        this.moviesFileScanner = moviesFileScanner;
        this.propertiesService = propertiesService;
    }

    // scan given paths and gather all files matching criteria
    // except ones that are already ignored or already has links
    @Override
    public void scanForNewMediaQueries() {
        List<MediaQuery> collect = moviesFileScanner.scanMediaFolders(
                        propertiesService.getTargetFolderListMovie(),
                        mediaTrackerDao.getAllMediaLinks()
                )
                .stream()
                .map(this::createQuery)
                .collect(Collectors.toList());
        setCurrentMediaQueries(collect);
        groupByParentPathBatch(getCurrentMediaQueries());
    }

    @Override
    public MediaQuery createQuery(Path filePath) {
        return new MediaQuery(filePath.toString(), MediaType.MOVIE);
    }

    @Override
    public void groupByParentPathBatch(List<MediaQuery> mediaQueryList) {
        List<FilePath> targetFolderListMovie = propertiesService.getTargetFolderListMovie();
        mediaQueriesByRootMap = mediaQueryList
                .stream()
                .filter(mq -> targetFolderListMovie.stream().noneMatch(
                        t -> t.getPath().equals(Path.of(mq.getFilePath()).getParent())
                ))
                .collect(Collectors.groupingBy(
                        this::getMatchingPath, Collectors.mapping(MediaQuery::getQueryUuid, Collectors.toList())
                ));
    }

    /*
     * Return root path that matches give media query element
     * If given query doesn't match any of provided paths empty path is returned
     * */
    @Override
    public Path getMatchingPath(MediaQuery mediaQuery) {
        List<FilePath> targetFolderListMovie = propertiesService.getTargetFolderListMovie();
        return targetFolderListMovie
                .stream()
                .map(FilePath::getPath)
                .filter(path -> Path.of(mediaQuery.getFilePath()).startsWith(path))
                .map(p -> p.getRoot().resolve(Path.of(mediaQuery.getFilePath()).subpath(0, p.getNameCount() + 1)))
                .findFirst()
                .orElse(Path.of(""));
    }

    /*
     * Group media query element ids by parent folder
     * */
//    @Override
//    public void groupByParentPath(MediaQuery mediaQuery, List<FilePath> targetFolderList) {
//        Path parent = Path.of(mediaQuery.getFilePath()).getParent();
//        if (targetFolderList.stream().noneMatch(target -> target.getPath().equals(parent))) {
//            List<UUID> uuids = (mediaQueriesByRootMap.get(parent) == null)
//                    ? new LinkedList<>()
//                    : mediaQueriesByRootMap.get(parent);
//            uuids.add(mediaQuery.getQueryUuid());
//            mediaQueriesByRootMap.put(parent, uuids);
//        }
//    }

    /*
     * Returns list of media queries of elements sharing the same folder at the same file tree level.
     * */
    @Override
    public List<MediaQuery> getGroupedQueriesWithId(UUID mediaQueryUuid) {
//        Path parent = Path.of(getQueryByUuid(mediaQueryUuid).getFilePath()).getParent();
        MediaQuery queryByUuid = getQueryByUuid(mediaQueryUuid);
        if (queryByUuid == null) return List.of();
        Path parentPath = getMatchingPath(queryByUuid);
        if (mediaQueriesByRootMap == null) return List.of(queryByUuid);
        List<UUID> uuids = mediaQueriesByRootMap.get(parentPath);
        if (uuids == null) return List.of(queryByUuid);
        return uuids.stream()
                .map(this::getQueryByUuid)
                // after creating link other files within the same folder are ignored, so they won't appear here
                .filter(query -> query.getMultipart() == -1)
                .collect(Collectors.toList());
    }

//    @Override
//    public List<MediaQuery> getProcessList() {
//        return groupedQueriesToProcess;
//    }
//
//    /*
//     * Adds single query to process list, it's called when new link is created
//     * */
//    @Override
//    public List<MediaQuery> addQueryToProcess(MediaQuery mediaQuery) {
//        // MediaType is never null
////        if (mediaQuery.getMediaType() == null) mediaQuery.setMediaType(MediaType.MOVIE);
//        groupedQueriesToProcess = List.of(mediaQuery);
//        return groupedQueriesToProcess;
//    }
//
//    @Override
//    public List<MediaQuery> addQueriesToProcess(List<MultiPartElement> multiPartElementsList) {
//        groupedQueriesToProcess = new LinkedList<>();
//        int counter = multiPartElementsList.size();
//        for (MediaQuery mq : getCurrentMediaQueries()) {
//            for (MultiPartElement mpe : multiPartElementsList) {
//                if (mpe.getMultipartSwitch() && mpe.getFilePath().equals(mq.getFilePath())) {
//                    mq.setMultipart(mpe.getPartNumber());
//                    mq.setMediaType(mpe.getMediaType());
//                    groupedQueriesToProcess.add(mq);
//                    counter--;
//                    break;
//                }
//            }
//            if (counter == 0) break;
//        }
//        LOG.info("[ query_service ] Grouped {} elements", groupedQueriesToProcess.size());
//        if (groupedQueriesToProcess.isEmpty()) {
//            LOG.info("[ query_service ] No queries marked, adding reference to the queue");
//            addQueryToProcess(getReferenceQuery());
//        }
//        return groupedQueriesToProcess;
//    }

}

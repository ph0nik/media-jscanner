package service.query;

import app.config.CacheConfig;
import dao.MediaTrackerDao;
import model.MediaQuery;
import model.path.FilePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import scanner.MediaFilesScanner;
import service.LiveDataService;
import service.Pagination;
import service.PropertiesService;
import util.MediaType;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Component("movieQuery")
public class MovieQueryService extends GeneralQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(MovieQueryService.class);
    private static final String MOVIE_QUERIES_LIST_KEY = "movie-queries-list";
    private final MediaTrackerDao mediaTrackerDao;
    private final MediaFilesScanner moviesFileScanner;
    private final PropertiesService propertiesService;
//    private Map<Path, List<UUID>> mediaQueriesByRootMap;

    public MovieQueryService(@Qualifier("jpa") MediaTrackerDao mediaTrackerDao,
                             MediaFilesScanner moviesFileScanner,
                             PropertiesService propertiesService,
                             Pagination<MediaQuery> pagination,
                             LiveDataService liveDataService,
                             CacheManager cacheManager) {
        super(pagination, cacheManager);
        this.mediaTrackerDao = mediaTrackerDao;
        this.moviesFileScanner = moviesFileScanner;
        this.propertiesService = propertiesService;
    }

    // scan given paths and gather all files matching criteria
    // except ones that are already ignored or already has links
    @Override
    public List<MediaQuery> scanForNewMediaQueries() {
        if (propertiesService.userPathsPresent()) {
            List<MediaQuery> collect = moviesFileScanner.scanMediaFolders(
                            propertiesService.getTargetFolderListMovie(),
                            mediaTrackerDao.getAllMediaLinks()
                    )
                    .stream()
                    .map(this::createMovieQuery)
                    .collect(Collectors.toList());
            updateCurrentMediaQueries(collect);
            return collect;
//            groupByParentPathBatch(getCurrentMediaQueries());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MediaQuery> getCurrentMediaQueries() {
        List<MediaQuery> fromCache = getFromCache(
                CacheConfig.MEDIA_QUERIES,
                MOVIE_QUERIES_LIST_KEY,
                List.class);
        return (fromCache == null) ? scanForNewMediaQueries() : fromCache;
    }

    @Override
    public void updateCurrentMediaQueries(List<MediaQuery> mediaQueryList) {
        updateCache(CacheConfig.MEDIA_QUERIES, MOVIE_QUERIES_LIST_KEY, mediaQueryList);
    }

    @Override
    public MediaQuery createMovieQuery(Path filePath) {
        return new MediaQuery(filePath.toString(), MediaType.MOVIE);
    }

    @Override
    public Map<Path, List<UUID>> groupByParentPathBatch(List<MediaQuery> mediaQueryList) {
        List<FilePath> targetFolderListMovie = propertiesService.getTargetFolderListMovie();
        Map<Path, List<UUID>> collect = mediaQueryList
                .stream()
                .filter(mq -> targetFolderListMovie.stream().noneMatch(
                        t -> t.getPath().equals(Path.of(mq.getFilePath()).getParent())
                ))
                .collect(Collectors.groupingBy(
                        this::getMatchingPath,
                        Collectors.mapping(MediaQuery::getQueryUuid, Collectors.toList())
                ));
        return collect;
//        liveDataService.setMediaQueriesByRootMap(collect); TODO
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
        // TODO rework
        MediaQuery queryByUuid = getQueryByUuid(mediaQueryUuid);
        if (queryByUuid == null) return List.of();
        Path parentPath = getMatchingPath(queryByUuid);
        Map<Path, List<UUID>> pathListMap = groupByParentPathBatch(getCurrentMediaQueries());
        if (pathListMap == null) return List.of(queryByUuid);
        List<UUID> uuids = pathListMap.get(parentPath);
        if (uuids == null) return List.of(queryByUuid);
        return uuids.stream()
                .map(this::getQueryByUuid)
                // after creating link other files within the same folder are ignored,
                // so they won't appear here
                .filter(query -> query.getMultipart() == -1)
                .sorted(Comparator.comparing(MediaQuery::getFilePath))
                .collect(Collectors.toList());
    }

}

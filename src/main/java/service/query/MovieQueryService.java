package service.query;

import app.config.CacheConfig;
import dao.MediaTrackerDao;
import model.MediaQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
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
    private static final String MOVIE_QUERIES_LIST_KEY = "movie-queries-list";
    private final MediaTrackerDao mediaTrackerDao;
    private final MediaFilesScanner moviesFileScanner;
    private final PropertiesService propertiesService;
//    private Map<Path, List<UUID>> mediaQueriesByRootMap;

    public MovieQueryService(@Qualifier("jpa") MediaTrackerDao mediaTrackerDao,
                             MediaFilesScanner moviesFileScanner,
                             PropertiesService propertiesService,
                             Pagination<MediaQuery> pagination,
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
        if (propertiesService.areMoviePathsProvided()) {
            List<MediaQuery> collect = moviesFileScanner.scanMediaFolders(
                            propertiesService.getSourceFolderListMovie(),
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
        List<Path> targetFolderListMovie = propertiesService.getSourceFolderListMovie();
        return mediaQueryList
                .stream()
                .filter(
                        mq -> targetFolderListMovie.stream().noneMatch(
                                t -> t.equals(Path.of(mq.getFilePath()).getParent())
                        )
                )
                .collect(
                        Collectors.groupingBy(
                                this::getMatchingParentPath,
                                Collectors.mapping(
                                        MediaQuery::getQueryUuid, Collectors.toList()
                                )
                        )
                );
//        liveDataService.setMediaQueriesByRootMap(collect); TODO
    }

    /*
     * Return parent path that matches given media query element
     * If given query doesn't match any of provided paths empty path is returned
     * */
    @Override
    public Path getMatchingParentPath(MediaQuery mediaQuery) {
        List<Path> sourceFolderListMovie = propertiesService.getSourceFolderListMovie();
        return sourceFolderListMovie
                .stream()
                .filter(
                        path -> Path.of(mediaQuery.getFilePath())
                                .startsWith(path)
                )
                .map(
                        p -> p.getRoot().resolve(Path.of(mediaQuery.getFilePath())
                                .subpath(0, p.getNameCount() + 1))
                )
                .findFirst()
                .orElse(Path.of(""));
    }

    /*
     * From given list of paths returns path that is common for all of them
     * */
    private Path findCommonPath(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return null;
        if (paths.size() == 1) {
            return paths.get(0).getParent() != null ? paths.get(0).getParent() : paths.get(0);
        }
        Path[] normalizedPaths = paths
                .stream()
                .map(Path::normalize)
                .toArray(Path[]::new);
        int minLength = paths
                .stream()
                .mapToInt(Path::getNameCount)
                .min()
                .orElse(0);
        Path firstPath = normalizedPaths[0];
        int commonLength = minLength;
        for (int i = 0; i < minLength; i++) {
            Path component = firstPath.getName(i);
            for (int j = 1; j < normalizedPaths.length; j++) {
                if (!normalizedPaths[j].getName(i).equals(component)) {
                    commonLength = i;
                    break;
                }
            }
            if (commonLength == i) {
                break;
            }
        }
        return commonLength == 0 ? null : firstPath.subpath(0, commonLength);
    }

    @Override
    public List<MediaQuery> extractParentPath(
            MediaQuery selectedMediaQuery,
            List<MediaQuery> mediaQueryList
    ) {
        Path selectedParent = Path.of(selectedMediaQuery.getFilePath()).getParent();
        List<Path> sourceRootArray = propertiesService
                .getSourceFolderListMovie();

        // if any of the root paths equals to given parent path
        if (sourceRootArray
                .stream()
                .anyMatch(path -> path.equals(selectedParent)))
            return List.of(selectedMediaQuery);

        // select root path that matches selected parent
        Path rootPathMatchingSelection = sourceRootArray
                .stream()
                .filter(selectedParent::startsWith)
                .findFirst()
                .orElse(null);
        if (rootPathMatchingSelection == null) return List.of(selectedMediaQuery);

        // get list of queries that starts with given root
        List<Path> queriesWithSelectedRoot = mediaQueryList
                .stream()
                .filter(mq -> Path.of(mq.getFilePath()).startsWith(rootPathMatchingSelection))
                .map(mq -> Path.of(mq.getFilePath()))
                .toList();

        // find common path for queries
        Path commonRootPath = findCommonPath(queriesWithSelectedRoot);

        return (selectedParent.equals(commonRootPath))
                ? List.of(selectedMediaQuery)
                : mediaQueryList
                .stream()
                .filter(path -> Path.of(path.getParentPath()).equals(selectedParent))
                .toList();
    }

    /*
     * Returns list of media queries of elements sharing the same folder at the same file tree level.
     * */
    @Override
    public List<MediaQuery> getGroupedQueriesWithId(UUID mediaQueryUuid) {
        MediaQuery queryByUuid = getQueryByUuid(mediaQueryUuid);
        return (queryByUuid == null)
                ? List.of()
                : extractParentPath(queryByUuid, getCurrentMediaQueries());
//        if (queryByUuid == null) return List.of();
//        Path parentPath = getMatchingParentPath(queryByUuid);
//        Map<Path, List<UUID>> pathListMap = groupByParentPathBatch(getCurrentMediaQueries());
//        if (pathListMap == null) return List.of(queryByUuid);
//        List<UUID> uuids = pathListMap.get(parentPath);
//        if (uuids == null) return List.of(queryByUuid);
//        return uuids.stream()
//                .map(this::getQueryByUuid)
//                // after creating link other files within the same folder are ignored,
//                // so they won't appear here
//                .filter(query -> query.getMultipart() == -1)
//                .sorted(Comparator.comparing(MediaQuery::getFilePath))
//                .collect(Collectors.toList());
    }

}

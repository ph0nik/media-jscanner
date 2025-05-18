package service.query;

import app.config.CacheConfig;
import model.MediaQuery;
import model.multipart.MultiPartElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import service.Pagination;
import service.exceptions.MissingReferenceMediaQueryException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class GeneralQueryService implements MediaQueryService {

    public static final Logger LOG = LoggerFactory.getLogger(GeneralQueryService.class);
    public static final String MEDIA_QUERY_REFERENCE = "media-query-reference";
    public static final String MEDIA_QUERY_PROCESS_LIST = "media-query-process";

    private final Pagination<MediaQuery> pagination;
    private final CacheManager cacheManager;

    public GeneralQueryService(
            Pagination<MediaQuery> pagination,
            CacheManager cacheManager
    ) {
        this.pagination = pagination;
        this.cacheManager = cacheManager;
    }

    @Override
    public MediaQuery getReferenceQuery() throws MissingReferenceMediaQueryException {
        MediaQuery fromCache = getFromCache(CacheConfig.MEDIA_QUERIES, MEDIA_QUERY_REFERENCE, MediaQuery.class);
        if (fromCache == null) throw new MissingReferenceMediaQueryException();
        else return fromCache;
    }

    @Override
    public void setReferenceQuery(UUID mediaQueryUuid) {
        updateCache(CacheConfig.MEDIA_QUERIES, MEDIA_QUERY_REFERENCE, getQueryByUuid(mediaQueryUuid));
    }

    @Override
    public void setReferenceQuery(MediaQuery mediaQuery) {
        updateCache(CacheConfig.MEDIA_QUERIES, MEDIA_QUERY_REFERENCE, mediaQuery);
    }

    protected void updateCache(String cacheName, String cacheKey, Object value) {
        if (value != null) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(cacheKey, value);
            }
        }
    }

    protected <T> T getFromCache(String cacheName, String cacheKey, Class<T> type) {
        Cache cache = cacheManager.getCache(cacheName);
        return (cache != null) ? cache.get(cacheKey, type) : null;
    }

    @Override
    public void removeQueryFromQueue(MediaQuery mediaQuery) {
        List<MediaQuery> filteredQueries = getCurrentMediaQueries()
                .stream()
                .filter(mq -> !mq.getQueryUuid().equals(mediaQuery.getQueryUuid()))
                .collect(Collectors.toList());
        updateCurrentMediaQueries(filteredQueries);
//        groupByParentPathBatch(filteredQueries);
    }

    @Override
    public void removeQueryByFilePath(String path) {
        List<MediaQuery> filteredQueries = getCurrentMediaQueries()
                .stream()
                .filter(mq -> !mq.getFilePath().equals(path))
                .collect(Collectors.toList());
        updateCurrentMediaQueries(filteredQueries);
//        groupByParentPathBatch(filteredQueries);
    }

    /*
     * Returns media query by its uuid
     * If element is not found returns null
     * */
    @Override
    public MediaQuery getQueryByUuid(UUID uuid) {
        return getCurrentMediaQueries()
                .stream()
                .filter(x -> x.getQueryUuid().equals(uuid))
                .findFirst().orElse(null);
    }

    @Override
    public List<MediaQuery> extractParentPath(MediaQuery selectedMediaQuery, List<MediaQuery> mediaQueryList) {
        return null; // TODO
    };

    @Override
    public abstract List<MediaQuery> getGroupedQueriesWithId(UUID mediaQueryUuid);

    @Override
    public List<MediaQuery> searchQuery(String search) {
        if (search == null || search.isEmpty()) return List.of();
        String[] words = search.toLowerCase().split(" ");
        return getCurrentMediaQueries()
                .stream()
                .filter(mq -> containsAllWords(words, mq.getFilePath()))
                .collect(Collectors.toList());
    }

    //    @Override
    public boolean containsAllWords(String[] words, String filePath) {
        return Arrays
                .stream(words)
                .allMatch(filePath.toLowerCase()::contains);
    }

    @Override
    public Page<MediaQuery> getPageableQueries(Pageable pageable, List<MediaQuery> mediaQueryList) {
        return pagination.getPage(pageable, mediaQueryList);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MediaQuery> getProcessList() {
        return getFromCache(CacheConfig.MEDIA_QUERIES, MEDIA_QUERY_PROCESS_LIST, List.class);
//        return Optional.ofNullable(cacheManager.getCache(CacheConfig.PROCESS_QUERIES))
//                .map(cache -> cache.get(MEDIA_QUERY_PROCESS_KEY, List.class))
//                .map(list -> (List<MediaQuery>) list)
//                .orElse(List.of());
//        return liveDataService.getGroupedQueriesToProcess();
    }

    @Override
    public List<MediaQuery> addQueryToProcess(MediaQuery mediaQuery) {
        updateCache(CacheConfig.MEDIA_QUERIES, MEDIA_QUERY_PROCESS_LIST, List.of(mediaQuery));
        return List.of(mediaQuery);
    }

    public List<MediaQuery> setGroupedQueriesToProcess(List<MediaQuery> groupedQueriesToProcess) {
        updateCache(CacheConfig.MEDIA_QUERIES, MEDIA_QUERY_PROCESS_LIST, groupedQueriesToProcess);
        return groupedQueriesToProcess;
    }

    @Override
    public List<MediaQuery> addQueriesToProcess(List<MultiPartElement> multiPartElementsList) {
        LinkedList<MediaQuery> groupedQueriesToProcess = new LinkedList<>();
        int counter = multiPartElementsList.size();
        for (MediaQuery mq : getCurrentMediaQueries()) {
            for (MultiPartElement mpe : multiPartElementsList) {
                if (mpe.getMultipartSwitch() && mpe.getFilePath().equals(mq.getFilePath())) {
                    mq.setMultipart(mpe.getPartNumber());
                    mq.setMediaType(mpe.getMediaType());
                    groupedQueriesToProcess.add(mq);
                    counter--;
                    break;
                }
            }
            if (counter == 0) break;
        } // TODO assuming this list has at least one element
        return setGroupedQueriesToProcess(groupedQueriesToProcess);

    }

    @Override
    public MediaQuery getQueryById(Long id) {
        return getCurrentMediaQueries()
                .stream()
                .filter(x -> x.getQueryId() == id)
                .findFirst()
                .orElse(null);
    }

    /*
     * get media query by path or else return null
     * */
    @Override
    public MediaQuery getMediaQueryByPath(Path path) {
        return getQueryByFilePath(path.toString());
    }

    @Override
    public MediaQuery getQueryByFilePath(String filepath) {
        return getCurrentMediaQueries()
                .stream()
                .filter(x -> x.getFilePath().equals(filepath))
                .findFirst()
                .orElse(null);
    }

    /*
     * Checks if media query is part of media set, needed for auto matcher wizard.
     * */
    @Override
    public boolean isMultipart(MediaQuery mediaQuery) {
        return getGroupedQueriesWithId(mediaQuery.getQueryUuid()).size() > 1;
    }
}

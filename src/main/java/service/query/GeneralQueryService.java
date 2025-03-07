package service.query;

import model.MediaQuery;
import model.multipart.MultiPartElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import service.Pagination;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class GeneralQueryService implements MediaQueryService {

    public static final Logger LOG = LoggerFactory.getLogger(GeneralQueryService.class);

    private final Pagination<MediaQuery> pagination;
    private List<MediaQuery> mediaQueriesList; // = new LinkedList<>();
    private MediaQuery referenceQuery;
    private List<MediaQuery> groupedQueriesToProcess;

    public GeneralQueryService(Pagination<MediaQuery> pagination) {
        this.pagination = pagination;
    }
    @Override
    public MediaQuery getReferenceQuery() {
        return referenceQuery;
    };

    @Override
    public void setReferenceQuery(UUID mediaQueryUuid) {
        referenceQuery = getQueryByUuid(mediaQueryUuid);
    }

    @Override
    public void setReferenceQuery(MediaQuery mediaQuery) {
        referenceQuery = mediaQuery;
    }

    @Override
    public abstract void scanForNewMediaQueries();

//    MediaQuery getTvQuery(Path filePath) {
//        return new MediaQuery(filePath.toString(), MediaType.TV);
//    }

//    public MediaFilesScanner initQueryList(MediaFilesScanner mediaFilesScanner) {
//        mediaQueriesList = new LinkedList<>();
//        return mediaFilesScanner;
//    }

//    @Override
//    public MediaQuery createQuery(String filepath, MediaType mediaType) {
//        MediaQuery mq = new MediaQuery(filepath, mediaType);
//        mq.setMediaType(mediaType);
//        return mq;
////        mediaQueriesList.add(mq);
//    }

    @Override
    public void removeQueryFromQueue(MediaQuery mediaQuery) {
        mediaQueriesList = getCurrentMediaQueries()
                .stream()
                .filter(mq -> !mq.getQueryUuid().equals(mediaQuery.getQueryUuid()))
                .collect(Collectors.toList());
        groupByParentPathBatch(mediaQueriesList);
    }

    @Override
    public void removeQueryByFilePath(String path) {
        mediaQueriesList = getCurrentMediaQueries()
                .stream()
                .filter(mq -> !mq.getFilePath().equals(path))
                .collect(Collectors.toList());
        groupByParentPathBatch(mediaQueriesList);
    }

    /*
    * Returns media query by its uuid
    * If element is not found returns null
    * */
    @Override
    public MediaQuery getQueryByUuid(UUID uuid) {
        return mediaQueriesList
                .stream()
                .filter(x -> x.getQueryUuid().equals(uuid))
                .findFirst().orElse(null);
    }

    @Override
    public abstract void groupByParentPathBatch(List<MediaQuery> mediaQueryList);

    @Override
    public abstract List<MediaQuery> getGroupedQueriesWithId(UUID mediaQueryUuid);

    @Override
    public List<MediaQuery> searchQuery(String search) {
        if (search == null || search.isEmpty()) return List.of();
        String[] words = search.toLowerCase().split(" ");
        return mediaQueriesList.stream()
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
    public List<MediaQuery> getCurrentMediaQueries() {
        return (mediaQueriesList == null) ? List.of() : mediaQueriesList;
    }

    /*
    * get media query by path or else return null
    * */
    @Override
    public MediaQuery getMediaQueryByPath(Path path) {
        return mediaQueriesList.stream()
                .filter(mq -> mq.getFilePath().equals(path.toString()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Page<MediaQuery> getPageableQueries(Pageable pageable, List<MediaQuery> mediaQueryList) {
        return pagination.getPage(pageable, mediaQueryList);
    }

    @Override
    public void setCurrentMediaQueries(List<MediaQuery> mediaQueries) {
        this.mediaQueriesList = mediaQueries;
    };

    @Override
    public List<MediaQuery> getProcessList() {
        return groupedQueriesToProcess;
    }

    @Override
    public List<MediaQuery> addQueryToProcess(MediaQuery mediaQuery) {
        groupedQueriesToProcess = List.of(mediaQuery);
        return groupedQueriesToProcess;
    };

    @Override
    public List<MediaQuery> addQueriesToProcess(List<MultiPartElement> multiPartElementsList) {
        groupedQueriesToProcess = new LinkedList<>();
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
        }
        if (groupedQueriesToProcess.isEmpty()) {
            addQueryToProcess(getReferenceQuery());
        }
        return groupedQueriesToProcess;
    }

    @Override
    public MediaQuery getQueryById(Long id) {
        return mediaQueriesList
                .stream()
                .filter(x -> x.getQueryId() == id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public MediaQuery findQueryByFilePath(String filepath) {
        return mediaQueriesList
                .stream()
                .filter(x -> x.getFilePath().equals(filepath))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean isMultipart(MediaQuery mediaQuery) {
        return getGroupedQueriesWithId(mediaQuery.getQueryUuid()).size() > 1;
    }
}

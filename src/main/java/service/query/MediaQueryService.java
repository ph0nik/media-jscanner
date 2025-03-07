package service.query;

import model.MediaQuery;
import model.multipart.MultiPartElement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface MediaQueryService {
    MediaQuery getReferenceQuery();

    void setReferenceQuery(UUID mediaQueryUuid);

    void setReferenceQuery(MediaQuery mediaQuery);

    // scan given paths and gather all files matching criteria
    // except ones that are already ignored or already has links
    void scanForNewMediaQueries();

    MediaQuery createQuery(Path path);
    /*
     * Removes given element from the query list
     * */
    void removeQueryFromQueue(MediaQuery mediaQuery);

    /*
     * Removes list element with given path
     * */
    void removeQueryByFilePath(String path);

    MediaQuery getQueryByUuid(UUID uuid);

    void groupByParentPathBatch(List<MediaQuery> mediaQueryList);

    Path getMatchingPath(MediaQuery mediaQuery);

    /*
     * Group media query element ids by parent folder
     * */
//    void groupByParentPath(MediaQuery mediaQuery, List<FilePath> targetFolderList);

    /*
     * Returns list of media queries of elements sharing the same folder at the same file tree level.
     * */
    List<MediaQuery> getGroupedQueriesWithId(UUID mediaQueryUuid);

    List<MediaQuery> searchQuery(String search);

    /*
     * Returns current media query list
     * */
    List<MediaQuery> getCurrentMediaQueries();

    MediaQuery getMediaQueryByPath(Path path);

    Page<MediaQuery> getPageableQueries(Pageable pageable, List<MediaQuery> mediaQueryList);

    void setCurrentMediaQueries(List<MediaQuery> mediaQueries);

    List<MediaQuery> getProcessList();

    /*
     * Adds single query to process list, it's called when new link is created
     * */
    List<MediaQuery> addQueryToProcess(MediaQuery mediaQuery);

    List<MediaQuery> addQueriesToProcess(List<MultiPartElement> multiPartElementsList);

    boolean isMultipart(MediaQuery mediaQuery);

    /*
     * Returns query with given id or null if no such query is found
     * */
    MediaQuery getQueryById(Long id);

    /*
     * Returns query with given file path
     * */
    MediaQuery findQueryByFilePath(String filepath);
}

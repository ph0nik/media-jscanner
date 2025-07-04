package service.query;

import model.MediaQuery;
import model.multipart.MultiPartElement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import service.exceptions.MissingReferenceMediaQueryException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MediaQueryService {

    MediaQuery getReferenceQuery() throws MissingReferenceMediaQueryException;

    void setReferenceQuery(UUID mediaQueryUuid);

    void setReferenceQuery(MediaQuery mediaQuery);


    // scan given paths and gather all files matching criteria
    // except ones that are already ignored or already has links
    List<MediaQuery> scanForNewMediaQueries();

    MediaQuery createMovieQuery(Path path);
    /*
     * Removes given element from the query list
     * */
    void removeQueryFromQueue(MediaQuery mediaQuery);

    /*
     * Removes list element with given path
     * */
    void removeQueryByFilePath(String path);

    MediaQuery getQueryByUuid(UUID uuid);

    Map<Path, List<UUID>> groupByParentPathBatch(List<MediaQuery> mediaQueryList);

    Path getMatchingParentPath(MediaQuery mediaQuery);

    /*
     * Group media query element ids by parent folder
     * */
//    void groupByParentPath(MediaQuery mediaQuery, List<FilePath> targetFolderList);

    List<MediaQuery> extractParentPath(MediaQuery selectedMediaQuery, List<MediaQuery> mediaQueryList);

    /*
     * Returns list of media queries of elements sharing the same folder at the same file tree level.
     * */
    List<MediaQuery> getGroupedQueriesWithId(UUID mediaQueryUuid);

    List<MediaQuery> searchQuery(String search);

    /*
     * Returns current media query list
     * */
    List<MediaQuery> getCurrentMediaQueries();

    void updateCurrentMediaQueries(List<MediaQuery> mediaQueryList);

    MediaQuery getMediaQueryByPath(Path path);

    boolean containsAllWords(String[] words, String filePath);

    Page<MediaQuery> getPageableQueries(Pageable pageable, List<MediaQuery> mediaQueryList);

//    void setCurrentMediaQueries(List<MediaQuery> mediaQueries);

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
    MediaQuery getQueryByFilePath(String filepath);
}

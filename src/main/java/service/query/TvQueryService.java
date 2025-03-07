package service.query;

import dao.MediaTrackerDao;
import model.MediaQuery;
import model.path.FilePath;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import scanner.MediaFilesScanner;
import service.Pagination;
import service.PropertiesService;
import service.exceptions.NoQueryFoundException;
import util.MediaType;
import util.TextExtractTools;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Component("tvQuery")
public class TvQueryService extends GeneralQueryService {
    private final MediaTrackerDao mediaTrackerDao;
    private final MediaFilesScanner tvFilesScanner;
    private final PropertiesService propertiesService;
    private Map<Path, List<Path>> mediaQueriesByRootMap;
//    private List<MediaQuery> groupedQueriesToProcess;

    // TODO prepare links to be execute before they are executed, present user with results beforehand

    public TvQueryService(@Qualifier("spring") MediaTrackerDao mediaTrackerDao,
                          MediaFilesScanner mediaFilesScanner,
                          PropertiesService propertiesService,
                          Pagination<MediaQuery> pagination) {
        super(pagination);
        this.mediaTrackerDao = mediaTrackerDao;
        this.tvFilesScanner = mediaFilesScanner;
        this.propertiesService = propertiesService;
    }

    // TODO
    // scan files and group by season folder
    // present list of folders
    // select folder
    // send search request with first file of given folder
    // present reasults and prompt for manual search
    // select title and season -- Tv Title [s01] [s02] [s03] ...
    // send request for title details and match episodes number with given season
    // get grouped files and create links for each individual file

    @Override
    public void scanForNewMediaQueries() {
        List<MediaQuery> collect = tvFilesScanner.scanMediaFolders(
                        propertiesService.getTargetFolderListTv(),
                        mediaTrackerDao.getAllMediaLinks()
                )
                .stream()
                .map(this::createQuery)
                .collect(Collectors.toList());
        setCurrentMediaQueries(collect);
        groupByParentPathBatch(getCurrentMediaQueries());
    }

    public List<MediaQuery> getParentFolders() {
        if (mediaQueriesByRootMap == null) {
            return List.of();
        }
        return mediaQueriesByRootMap.keySet()
                .stream()
                .map(pf -> new MediaQuery(pf.toString(), MediaType.TV))
                .collect(Collectors.toList());
    }

    @Override
    public MediaQuery createQuery(Path path) {
        return new MediaQuery(path.toString(), MediaType.TV);
    }

    @Override
    public void groupByParentPathBatch(List<MediaQuery> mediaQueryList) {
        List<Path> collect = mediaQueryList
                .stream()
                .map(MediaQuery::getFilePath)
                .map(Path::of)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        List<Path> rootPaths = propertiesService.getTargetFolderListTv()
                .stream()
                .map(FilePath::getPath)
                .collect(Collectors.toList());
        mediaQueriesByRootMap = findCommonFolderForSortedPaths(collect, rootPaths);
    }

    Map<Path, List<Path>> findCommonFolderForSortedPaths(List<Path> path, List<Path> rootPaths) {
        Map<Path, List<Path>> groupedMap = new LinkedHashMap<>();
        if (path.isEmpty()) return groupedMap;
        Path candidateParent = path.get(0).getParent();
        // count episodes per parent
        // if parent dont match and number is 1 or greater than 2
        // then new season and new parent
        List<Path> currentFolder = new LinkedList<>();
        for (Path p : path) {
            int rootDepth = rootPaths.stream()
                    .filter(p::startsWith)
                    .findFirst()
                    .orElse(Path.of(""))
                    .getNameCount();
            int parentNameCount = candidateParent.getNameCount();
            while (parentNameCount > rootDepth) {
                candidateParent = candidateParent
                        .getRoot()
                        .resolve(candidateParent.subpath(0, parentNameCount--));
                if (p.startsWith(candidateParent)) {
                    currentFolder.add(p);
                    parentNameCount = -1;
                }
                if (currentFolder.size() > 1 && parentNameCount > 0) {
                    parentNameCount = rootDepth;
                }
            }
            if (parentNameCount == rootDepth) {
                groupedMap.put(candidateParent, currentFolder);
                currentFolder = new LinkedList<>();
                currentFolder.add(p);
                candidateParent = p.getParent();
            }
        }
        groupedMap.put(candidateParent, currentFolder);
        return groupedMap;
    }

    /*
    * Extract season number from file name if possible
    * Returns -1 in case of not matching right pattern.
    * */
    public int getSeasonTv() {
        return TextExtractTools.extractSeasonNumber(getReferenceQuery().getFilePath());
    }

    /*
    * Not needed here TODO
    * */
    @Override
    public Path getMatchingPath(MediaQuery mediaQuery) {
        List<FilePath> targetFolderListTv = propertiesService.getTargetFolderListTv();
        return targetFolderListTv
                .stream()
                .map(FilePath::getPath)
                .filter(path -> Path.of(mediaQuery.getFilePath()).startsWith(path))
                .map(p -> Path.of(mediaQuery.getParentPath()))
                .findFirst()
                .orElse(Path.of(""));
    }

    /*
    * Get grouped queries with given parent folder path
    * */
    public List<MediaQuery> getGroupedQueriesWithParent(String parentPath) {
        return mediaQueriesByRootMap.get(Path.of(parentPath))
                .stream()
                .map(this::getMediaQueryByPath)
                .sorted(Comparator.comparing(MediaQuery::getFilePath))
                .collect(Collectors.toList());
    }

    public List<MediaQuery> getGroupedQueriesWithChild(String childPath) {
        String matchedParent = mediaQueriesByRootMap.keySet()
                .stream()
                .map(Path::toString)
                .filter(childPath::startsWith)
                .findFirst()
                .orElse(null);
        if (matchedParent != null) {
            return getGroupedQueriesWithParent(matchedParent);
        }
        return List.of();
    }

    /*
    * Get queries grouped together with query having given id
    * */
    @Override
    public List<MediaQuery> getGroupedQueriesWithId(UUID mediaQueryUuid) {
        MediaQuery queryByUuid = getQueryByUuid(mediaQueryUuid);
        for (Path parent : mediaQueriesByRootMap.keySet()) {
            boolean match = mediaQueriesByRootMap.get(parent)
                    .stream()
                    .anyMatch(fp -> fp.toString().equals(queryByUuid.getFilePath()));
            if (match) {
                return getGroupedQueriesWithParent(parent.toString());
            }
        }
        return List.of();
    }

    /*
    * With given parent path gets first associated element from grouped collection,
    * order is not guaranteed. File is used as a reference instead of parent path
    * because it is needed for details extraction.
    * */
    public void setUpQueryReference(String path) throws NoQueryFoundException {
        Iterator<Path> iterator = mediaQueriesByRootMap.get(Path.of(path)).iterator();
        if (iterator.hasNext()) {
            setReferenceQuery(getMediaQueryByPath(iterator.next()));
        } else {
            throw new NoQueryFoundException("No grouped queries found");
        }

    }
}

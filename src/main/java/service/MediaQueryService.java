package service;

import dao.MediaTrackerDao;
import model.MediaQuery;
import model.multipart.MultiPartElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import scanner.MediaFilesScanner;
import util.MediaType;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Component
public class MediaQueryService {

    private List<MediaQuery> mediaQueriesList = new LinkedList<>();

    private List<MediaQuery> groupedQueriesToProcess;

    private MediaQuery referenceQuery;
    private final Map<Path, List<UUID>> mediaQueriesByRootMap = new HashMap<>();

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
        candidates.forEach(c -> addQueryToQueue(c.toString()));
    }

    public MediaQuery addQueryToQueue(String filepath) {
        MediaQuery mq = new MediaQuery(filepath);
        mediaQueriesList.add(mq);
        groupByParentPath(mq);
        return mq;
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
        Path parent = Path.of(getQueryByUuid(mediaQueryUuid).getFilePath()).getParent();
        List<MediaQuery> output = mediaQueriesByRootMap.get(parent)
                .stream()
                .map(this::getQueryByUuid)
                // after creating link other files within the same folder are ignored, so they won't appear here
                .filter(query -> query.getMultipart() == -1)
                .collect(Collectors.toList());
        return output;
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

    // TODO move selected elements to temporary list
    public List<MediaQuery> addQueriesToProcess(List<MultiPartElement> mediaQueryList) {
        groupedQueriesToProcess = new LinkedList<>();
        for (MediaQuery current : mediaQueriesList) {
            for (MultiPartElement mpe : mediaQueryList) {
                if (mpe.getFilePath().equals(current.getFilePath()) && mpe.getMultipartSwitch() != 0) {
                    if (mpe.getMultipartSwitch() == 1 && mpe.getPartNumber() != 0) {
                        current.setMultipart(mpe.getPartNumber());
                    } else {
                        current.setMultipart((byte) -1);
                    }
                    current.setMediaType(mpe.getMediaType());
                    groupedQueriesToProcess.add(current);
                }
            }
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

    /*
     * Removes given element from the query list
     * */
    public void removeQueryFromQueue(MediaQuery mediaQuery) {
        mediaQueriesList = getCurrentMediaQueries()
                .stream()
                .filter(mq -> !mq.getQueryUuid().equals(mediaQuery.getQueryUuid()))
                .collect(Collectors.toList());
    }

    /*
     * Removes list element with given path
     * */
    public void removeQueryByFilePath(String path) {
        mediaQueriesList = getCurrentMediaQueries()
                .stream()
                .filter(x -> !x.getFilePath().equals(path))
                .collect(Collectors.toList());
    }

    public MediaQuery getQueryByUuid(UUID uuid) {
        Optional<MediaQuery> first = mediaQueriesList
                .stream()
                .filter(x -> x.getQueryUuid().equals(uuid))
                .findFirst();
        return first.orElse(null);
    }

    // TODO detect multiple parts
    // /share/download/random-sources/Running Scared/Running.Scared.2006.HDDVD.1080p.x264.DTS-NiX-2.mkv
    // /share/download/random-sources/Running Scared/Running.Scared.2006.HDDVD.1080p.x264.DTS-NiX-1.mkv
    // mediaQuery.multipart, 0 for none, 1 ... 99 for part number

    static void testMultipleParts(List<String> list) {
        Map<Path, List<String>> collect = list.stream()
                .collect(groupingBy(s -> Path.of(s).getParent()));
        for (Path p : collect.keySet()) {
            // if key has more than one entry
            System.out.println(collect.get(p));
            if (collect.get(p).size() > 1) {
                for (int i = 0; i < collect.get(p).size(); i++) {
                    // take i-th string in collection
                    String temp = collect.get(p).get(i);
                    // compare to every other one

                }
//                for (String str : collect.get(p)) {
//                    System.out.println(str);
//                }
            }
        }

    }

    static void compareString(String comparingTo, String comparingWith) {
        char[] to = comparingTo.toCharArray();
        char[] with = comparingWith.toCharArray();
        int sameChars = 0;
        if (to.length == with.length) {
            for (int i = 0; i < to.length; i++) {
                if (to[i] == with[i]) sameChars++;
            }
        }
        System.out.println("same chars: " + comparingTo + " " + sameChars);
        System.out.println("difference: " + comparingWith + " " + (to.length - sameChars));
    }

    public static void main(String[] args) {
        String part1 = "/share/download/random-sources/Running Scared/Running.Scared.2006.HDDVD.1080p.x264.DTS-NiX-2.mkv";
        String part2 = "/share/download/random-sources/Running Scared/Running.Scared.2006.HDDVD.1080p.x264.DTS-NiX-1.mkv";
        String part3 = "/share/download/random-sources/Kin-dza-dza_1986.1080p.BluRay.DTS.x264.HDCLUB-SbR/Kin-dza-dza_.Part.Two.1986.1080p.BluRay.DTS.x264.HDCLUB-SbR.mkv";
        String part4 = "/share/download/random-sources/Master.and.Commander.The.Far.Side.of.the.World.2003.720p.BluRay.DTS.x264-ESiR.sample.mkv";
        String part5 = "/share/download/random-sources/War.of.the.Worlds.2005.720p.BluRay.DTS.x264-ESiR.mkv";
        System.out.println(Path.of(part5.replaceAll("/share/download/random-sources", "")).getRoot());
        List<String> part11 = List.of(part1, part2, part3, part4, part5);
        testMultipleParts(part11);
    }

}

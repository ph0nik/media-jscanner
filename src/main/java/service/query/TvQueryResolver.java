package service.query;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TvQueryResolver {

    private MovieQueryService movieQueryService;

    public TvQueryResolver(MovieQueryService movieQueryService) {
        this.movieQueryService = movieQueryService;
    }

    void groupEpisodes(List<String> fileList) {
        // group all the files withing subdirectory
        // take file path
        // extract parent folder beneath root
        // match all files from the same folder and group them
        // repeat

    }

    public List<String> getFoundShowsList(List<Path> rootPath, List<String> filePaths) {
        return batchPathExtraction(rootPath, filePaths)
                .keySet()
                .stream()
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    public Map<Path, List<Path>> batchPathExtraction(List<Path> rootPath, List<String> filePaths) {
        return filePaths.stream()
                .map(Path::of)
                .map(fp -> extractParentSecondToRoot(rootPath, fp))
                .collect(Collectors.groupingBy(fp -> fp.getName(0)));
    }

    /*
    * Get all files with matching parent folder
    * */
    public List<Path> getFileNameWithParentFolder(String parentFolder, List<String> filePaths) {
        return filePaths.stream()
                .filter(p -> p.contains(parentFolder))
                .map(Path::of)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    /*
    * Extract name of parent folder of the series
    * */
    public List<String> extractMainSeriesFolder(List<Path> rootPath, List<String> filePaths) {
        return filePaths.stream()
                .map(Path::of)
                .map(fp -> extractParentSecondToRoot(rootPath, fp).getName(0).toString())
                .distinct()
                .collect(Collectors.toList());
    }

    public Path extractParentSecondToRoot(List<Path> rootPath, Path filePath) {
        return rootPath.stream()
                .filter(filePath::startsWith)
                .map(rp -> rp.relativize(filePath))
                .findFirst()
                .orElse(null);
    }

}

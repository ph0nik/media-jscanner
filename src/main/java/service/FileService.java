package service;

import model.MediaTransferData;
import model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import util.MediaIdentity;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import static util.TextExtractTools.*;
import static util.TextExtractTools.replaceIllegalCharacters;

@Service
public class FileService {

    private static final Logger LOG = LoggerFactory.getLogger(FileService.class);
    private static final String WHITESPACE = " ";

    private Path linksRootFolder;

    public void setLinksRootFolder(Path root) {
        this.linksRootFolder = root;
    }

    /*
     * Create file path for symlink file with given query result and media data
     * */
    public Path createMovieLinkPath(QueryResult queryResult, MediaTransferData mediaTransferData,
                             MediaIdentity mediaIdentity) throws FileNotFoundException {
        if (linksRootFolder == null) throw new FileNotFoundException("No links root folder defined.");
        String imdbPattern = "[imdbid-%imdb_id%]";
        int discNumber = mediaTransferData.getPartNumber();
        String part = (discNumber > 0) ? "-cd" + discNumber : "";
        String title = replaceIllegalCharacters(mediaTransferData.getTitle());
        String yearFormatted = WHITESPACE + "(" + mediaTransferData.getYear() + ")";
        String idFormatted = "";
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + queryResult.getTheMovieDbId() + "]";
//            idFormatted = imdbPattern.replaceAll("%imdb_id%", queryResult.getImdbId());
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + mediaTransferData.getImdbId() + "]";
        }
        String extension = getExtension(queryResult.getOriginalPath());
        // get special identifier for movie extras
        String special = checkForSpecialDescriptor(queryResult.getOriginalPath());
        String group = ""; //
        String specialWithGroup = (special + WHITESPACE + group).trim();
        specialWithGroup = (specialWithGroup.trim().isEmpty()) ? "" : WHITESPACE + "-" + WHITESPACE + "[" + specialWithGroup + "]";
        // build path names
        LOG.info("[ link ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        LOG.info("[ link ] folder: {}", movieFolder);
        String movieName = title + specialWithGroup + part + "." + extension;
        LOG.info("[ link ] file: {}", movieName);
        return linksRootFolder.resolve(movieFolder).resolve(movieName);
    }


    public Path createExtrasLinkPath(QueryResult queryResult, MediaTransferData mediaTransferData,
                                      MediaIdentity mediaIdentity) throws FileNotFoundException {
        if (linksRootFolder == null) throw new FileNotFoundException("No links root folder defined.");
        String title = replaceIllegalCharacters(mediaTransferData.getTitle());

        int year = mediaTransferData.getYear();
        String yearFormatted = WHITESPACE + "(" + year + ")";

        String imdbId = mediaTransferData.getImdbId();
        int tmdbId = queryResult.getTheMovieDbId();
        String idFormatted = "";
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + tmdbId + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + imdbId + "]";
        }
        Path of = Path.of(queryResult.getOriginalPath());
        Path fileName = of.getName(of.getNameCount() - 1);

        // build path names
        LOG.info("[ link ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        String extrasFolder = "extras";
        String movieName = fileName.toString();
//        return Path.of(LINKS_ROOT).resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
        return linksRootFolder.resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
    }

    public boolean validatePath(String path) {
        return Path.of(path).toFile().exists();
    }
}

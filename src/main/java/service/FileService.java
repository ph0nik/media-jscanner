package service;

import model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import util.MediaIdentity;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import static util.TextExtractTools.*;

@Service
public class FileService {
    private static final Logger LOG = LoggerFactory.getLogger(FileService.class);
    private final String whitespace = " ";
    private Path linksRootFolder;
    public void setLinksRootFolder(Path root) {
        this.linksRootFolder = root;
    }

    /*
     * Create file path for symlink file with given query result and media data
     * */
//    public Path createMovieLinkPath(QueryResult queryResult, MediaTransferData mediaTransferData,
//                             MediaIdentity mediaIdentity) throws FileNotFoundException {
//        if (linksRootFolder == null) throw new FileNotFoundException("No links root folder defined.");
//        String imdbPattern = "[imdbid-%imdb_id%]";
//        int discNumber = mediaTransferData.getPartNumber();
//        String part = (discNumber > 0) ? "-cd" + discNumber : "";
//        String title = replaceIllegalCharacters(mediaTransferData.getTitle());
//        String yearFormatted = WHITESPACE + "(" + mediaTransferData.getYear() + ")";
//        String idFormatted = "";
//        if (mediaIdentity == MediaIdentity.TMDB) {
//            idFormatted = " [tmdbid-" + queryResult.getTheMovieDbId() + "]";
////            idFormatted = imdbPattern.replaceAll("%imdb_id%", queryResult.getImdbId());
//        }
//        if (mediaIdentity == MediaIdentity.IMDB) {
//            idFormatted = " [imdbid-" + mediaTransferData.getImdbId() + "]";
//        }
//        String extension = getExtension(queryResult.getOriginalPath());
//        // get special identifier for movie extras
//        String special = checkForSpecialDescriptor(queryResult.getOriginalPath());
//        String group = ""; //
//        String specialWithGroup = (special + WHITESPACE + group).trim();
//        specialWithGroup = (specialWithGroup.trim().isEmpty()) ? "" : WHITESPACE + "-" + WHITESPACE + "[" + specialWithGroup + "]";
//        // build path names
//        LOG.info("[ link ] creating path names...");
//        String movieFolder = title + yearFormatted + idFormatted;
//        LOG.info("[ link ] folder: {}", movieFolder);
//        String movieName = title + specialWithGroup + part + "." + extension;
//        LOG.info("[ link ] file: {}", movieName);
//        return linksRootFolder.resolve(movieFolder).resolve(movieName);
//    }

    public Path createMovieLinkPath(QueryResult queryResult,
                                    MediaIdentity mediaIdentity) throws FileNotFoundException {
        if (linksRootFolder == null) throw new FileNotFoundException("No links root folder defined.");
        String imdbPattern = "[imdbid-%imdb_id%]";
        int discNumber = queryResult.getMultipart();
        String part = (discNumber > 0) ? "-cd" + discNumber : "";
        if (queryResult.getTitle() == null || queryResult.getTitle().isEmpty()) {
            LOG.error("[ file_service ] Empty title field.");
            return null;
        }
        String title = replaceIllegalCharacters(queryResult.getTitle());
        if (queryResult.getYear() == null || queryResult.getYear().isEmpty()) {
            LOG.error("[ file_service ] Empty year field");
            return null;
        }
        String yearFormatted = whitespace + "(" + queryResult.getYear() + ")";
        String idFormatted = "";
        if (mediaIdentity == null) {
            LOG.error("[ file_service ] No media identity provided");
            return null;
        }
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + queryResult.getTheMovieDbId() + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + queryResult.getImdbId() + "]";
        }
        if (queryResult.getOriginalPath() == null || queryResult.getOriginalPath().isEmpty()) {
            LOG.error("[ file_service ] Original path not found");
            return null;
        }
        String extension = getExtension(queryResult.getOriginalPath());
        // get special identifier for movie extras
        String special = checkForSpecialDescriptor(queryResult.getOriginalPath());
        String group = ""; //
        String specialWithGroup = (special + whitespace + group).trim();
        specialWithGroup = (specialWithGroup.trim().isEmpty()) ? "" : whitespace + "-" + whitespace + "[" + specialWithGroup + "]";
        // build path names
        LOG.info("[ file_service ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        LOG.info("[ file_service ] folder: {}", movieFolder);
        String movieName = title + specialWithGroup + part + "." + extension;
        LOG.info("[ file_service ] file: {}", movieName);
        return linksRootFolder.resolve(movieFolder).resolve(movieName);
    }


//    public Path createExtrasLinkPath(QueryResult queryResult, MediaTransferData mediaTransferData,
//                                      MediaIdentity mediaIdentity) throws FileNotFoundException {
//        if (linksRootFolder == null) throw new FileNotFoundException("No links root folder defined.");
//        String title = replaceIllegalCharacters(mediaTransferData.getTitle());
//
//        int year = mediaTransferData.getYear();
//        String yearFormatted = WHITESPACE + "(" + year + ")";
//
//        String imdbId = mediaTransferData.getImdbId();
//        int tmdbId = queryResult.getTheMovieDbId();
//        String idFormatted = "";
//        if (mediaIdentity == MediaIdentity.TMDB) {
//            idFormatted = " [tmdbid-" + tmdbId + "]";
//        }
//        if (mediaIdentity == MediaIdentity.IMDB) {
//            idFormatted = " [imdbid-" + imdbId + "]";
//        }
//        Path of = Path.of(queryResult.getOriginalPath());
//        Path fileName = of.getName(of.getNameCount() - 1);
//
//        // build path names
//        LOG.info("[ link ] creating path names...");
//        String movieFolder = title + yearFormatted + idFormatted;
//        String extrasFolder = "extras";
//        String movieName = fileName.toString();
////        return Path.of(LINKS_ROOT).resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
//        return linksRootFolder.resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
//    }

    public Path createExtrasLinkPath(QueryResult queryResult,
                                     MediaIdentity mediaIdentity) throws FileNotFoundException {
        if (linksRootFolder == null) throw new FileNotFoundException("No links root folder defined.");
        if (queryResult.getTitle() == null || queryResult.getTitle().isEmpty()) {
            LOG.error("[ file_service ] Empty title field.");
            return null;
        }
        String title = replaceIllegalCharacters(queryResult.getTitle());
        if (queryResult.getYear() == null || queryResult.getYear().isEmpty()) {
            LOG.error("[ file_service ] Empty year field");
            return null;
        }
        String yearFormatted = whitespace + "(" + queryResult.getYear() + ")";
        String idFormatted = "";
        if (mediaIdentity == null) {
            LOG.error("[ file_service ] No media identity provided");
            return null;
        }
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = " [tmdbid-" + queryResult.getTheMovieDbId() + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = " [imdbid-" + queryResult.getImdbId() + "]";
        }
        if (queryResult.getOriginalPath() == null || queryResult.getOriginalPath().isEmpty()) {
            LOG.error("[ file_service ] Original path not found");
            return null;
        }
        Path of = Path.of(queryResult.getOriginalPath());
        Path fileName = of.getName(of.getNameCount() - 1);

        // build path names
        LOG.info("[ file_service ] creating path names...");
        String movieFolder = title + yearFormatted + idFormatted;
        String extrasFolder = "extras";
        String movieName = fileName.toString();
//        return Path.of(LINKS_ROOT).resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
        return linksRootFolder.resolve(movieFolder).resolve(extrasFolder).resolve(movieName);
    }

    public Path createTvEpisodePath(QueryResult queryResult, int seasonNumber,
                                    int episodeNumber, MediaIdentity mediaIdentity)
            throws FileNotFoundException {
        if (linksRootFolder == null) throw new FileNotFoundException("No links root folder defined.");
        if (queryResult.getMultipart() < episodeNumber) return null;
        if (queryResult.getTitle() == null || queryResult.getTitle().isEmpty()) {
            LOG.error("[ file_service ] Empty title field.");
            return null;
        }
        if (queryResult.getYear() == null || queryResult.getYear().isEmpty()) {
            LOG.error("[ file_service ] Empty year field");
            return null;
        }
        if (mediaIdentity == null) {
            LOG.error("[ file_service ] No media identity provided");
            return null;
        }
        if (queryResult.getOriginalPath() == null || queryResult.getOriginalPath().isEmpty()) {
            LOG.error("[ file_service ] Original path not found");
            return null;
        }
        String title = replaceIllegalCharacters(queryResult.getTitle());
        String yearFormatted = whitespace + "(" + queryResult.getYear() + ")";
        String season = (seasonNumber < 10) ? "0" + seasonNumber : Integer.toString(seasonNumber);
        String episode = (episodeNumber < 10) ? "E0" + episodeNumber : "E" + episodeNumber;
        String idFormatted = "";
        if (mediaIdentity == MediaIdentity.TMDB) {
            idFormatted = whitespace + "[tmdbid-" + queryResult.getTheMovieDbId() + "]";
        }
        if (mediaIdentity == MediaIdentity.IMDB) {
            idFormatted = whitespace + "[imdbid-" + queryResult.getImdbId() + "]";
        }
        String extension = getExtension(queryResult.getOriginalPath());
        // build path names
        LOG.info("[ file_service ] creating path names...");
        String titleFolder = title + yearFormatted + idFormatted;
        String seasonFolder = "Season" + whitespace + season;
        LOG.info("[ file_service ] folder: {}", titleFolder);
        String episodeName = title + whitespace + "S" + season + episode + "." + extension;
        LOG.info("[ file_service ] file: {}", episodeName);
        return linksRootFolder.resolve(titleFolder).resolve(seasonFolder).resolve(episodeName);
    }

    public boolean validatePath(String path) {
        return Path.of(path).toFile().exists();
    }
}

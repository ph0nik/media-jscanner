package service;

import model.QueryResult;
import model.validator.RequiredFieldException;
import model.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import util.MediaIdentity;

import java.nio.file.Path;

import static util.TextExtractTools.*;

@Service
public class FileService {
    private static final Logger LOG = LoggerFactory.getLogger(FileService.class);
    private static final String FOLDER_PATTERN = "%title% (%year%) [imdbid-%imdb_id%]";
    private static final String FILE_NAME_PATTERN = "%title%%special%%part%.%extension%";
    private final String whitespace = " ";

    public Path createMovieLinkPath_new(QueryResult queryResult,
                                         MediaIdentity mediaIdentity,
                                         Path rootLinksFolder) throws RequiredFieldException, IllegalAccessException {
        // query result validation should be outside of this method
        // TITLE (YEAR) [imdbid-IMDBID]/TITLE - [SPECIAL]-cdPART.EXT
        // TODO if one of ids is missing select the second one
        Validator.validateForNulls(queryResult); // check for null fields
        String part = (queryResult.getMultipart() > 0)
                ? "-cd" + queryResult.getMultipart()
                : "";
        String title = replaceIllegalCharacters(queryResult.getTitle());
        String movieId = (mediaIdentity == MediaIdentity.IMDB)
                ? queryResult.getImdbId()
                : String.valueOf(queryResult.getTheMovieDbId());
        String extension = getExtension(queryResult.getOriginalPath());
        String special = checkForSpecialDescriptor(queryResult.getOriginalPath());
        String specialWithGroup = (special.trim().isEmpty())
                ? ""
                : whitespace + "-" + whitespace + "[" + special + "]";
        String folderName = FOLDER_PATTERN.replace("%title%", title)
                .replace("%year%", queryResult.getYear())
                .replace("%imdb_id%", movieId);
        String fileName = FILE_NAME_PATTERN.replace("%title%", title)
                .replace("%special%", specialWithGroup)
                .replace("%part%", part)
                .replace("%extension%", extension);
        return rootLinksFolder.resolve(folderName).resolve(fileName);
    }

    public Path createMovieLinkPath(QueryResult queryResult,
                                    MediaIdentity mediaIdentity,
                                    Path linksRootFolder) throws RequiredFieldException, IllegalAccessException {
        Validator.validateForNulls(queryResult);
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
                                     MediaIdentity mediaIdentity,
                                     Path linksRootFolder) {
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
                                    int episodeNumber, MediaIdentity mediaIdentity,
                                    Path linksRootFolder) {
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

    public boolean doesPathExist(String path) {
        return Path.of(path).toFile().exists();
    }
}

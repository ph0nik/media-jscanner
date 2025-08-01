package app.controller;

import dao.DatabaseMigrationService;
import model.form.LinksPathForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.MediaLinksService;
import service.PropertiesService;
import service.exceptions.ConfigurationException;
import service.exceptions.MissingFolderOrFileException;
import service.exceptions.NoApiKeyException;
import util.MediaFilter;
import util.MediaType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Controller
public class ConfigController {
    private static final String CLEAR_FOLDERS_MOVIE_CONFIRM = "/config/clear-folders-movie-confirm/";
    private static final String CLEAR_FOLDERS_MOVIE_ABORT = "/config/clear-folders-movie-abort/";
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private DatabaseMigrationService databaseMigrationService;
    private static final String MOVIE_NEW_LINK_PATH = "/config/movie-new-link-path/";
    private static final String MOVIE_NEW_SOURCE_PATH = "/config/movie-new-source-path/";
    private static final String MOVIE_DELETE_SOURCE_PATH = "/config/movie-delete-source-path/";
    private static final String TV_NEW_LINK_PATH = "/config/tv-new-link-path/";
    private static final String TV_NEW_SOURCE_PATH = "/config/tv-new-source-path/";
    private static final String TV_DELETE_SOURCE_PATH = "/config/tv-delete-source-path/";
    private static final String CLEAR_FOLDERS_MOVIE = "/config/clear-folders/";
    private static final String BACKUP_DATABASE = "/config/backup-database/";
    private String webPagePosition = "";
    private static final String POSITION_TV = "tv";
    private static final String POSITION_MOVIE = "movie";
    private static final String POSITION_API_KEY = "api_key";
    private static final String POSITION_BACKUP = "backup";
    private static final String POSITION_EXTENSIONS = "extensions";

    @ModelAttribute
    private void setConfigEndpoints(Model model) {
        model.addAttribute("movie_new_link", MOVIE_NEW_LINK_PATH);
        model.addAttribute("movie_new_source", MOVIE_NEW_SOURCE_PATH);
        model.addAttribute("movie_delete_target", MOVIE_DELETE_SOURCE_PATH);
        model.addAttribute("tv_new_link", TV_NEW_LINK_PATH);
        model.addAttribute("tv_new_source", TV_NEW_SOURCE_PATH);
        model.addAttribute("tv_delete_target", TV_DELETE_SOURCE_PATH);
        model.addAttribute("clear_folders", CLEAR_FOLDERS_MOVIE);
        model.addAttribute("confirm_clear_folders", CLEAR_FOLDERS_MOVIE_CONFIRM);
        model.addAttribute("abort_clear_folders", CLEAR_FOLDERS_MOVIE_ABORT);
        model.addAttribute("backup_database", BACKUP_DATABASE);
        model.addAttribute("current_menu", 5);
    }

    @ModelAttribute("extensions")
    public List<String> getCurrentExtensions() {
        return MediaFilter.getExtensions();
    }

    /*
     * Returns configuration panel
     * */
    @GetMapping(value = CommonHandler.CONFIG)
    public String configuration(Model model) {
        webPagePosition = "top";
        Path movieLinksPath = propertiesService.getLinksFolderMovie();
        Path tvLinksPath = propertiesService.getLinksFolderTv();
        model.addAttribute("links_folder_movie", movieLinksPath.toString());
        model.addAttribute("links_folder_movie_exists", movieLinksPath.toFile().exists());
        model.addAttribute("links_folder_tv", tvLinksPath.toString());
        model.addAttribute("links_folder_tv_exists", tvLinksPath.toFile().exists());
        model.addAttribute("target_folder_movie", propertiesService.getSourcePathsDto(MediaType.MOVIE));
        model.addAttribute("target_folder_tv", propertiesService.getSourcePathsDto(MediaType.TV));
        model.addAttribute("links_path_form", new LinksPathForm());
        model.addAttribute("current_position", webPagePosition);
        model.addAttribute("api_key", propertiesService.getApiKeyPartial());
        return "config";
    }

    /*
     * Delete selected target path for movie
     * */
    @PostMapping(value = MOVIE_DELETE_SOURCE_PATH)
    public String deletePathMovie(@RequestParam String path, Model model)
            throws ConfigurationException {
        propertiesService.removeTargetPathMovie(Path.of(path));
        webPagePosition = POSITION_MOVIE;
        return "redirect:" + CommonHandler.CONFIG;
    }

    /*
     * Delete selected target path for tv
     * */
    @PostMapping(value = TV_DELETE_SOURCE_PATH)
    public String deletePathTv(@RequestParam String path, Model model)
            throws ConfigurationException {
        propertiesService.removeTargetPathTv(Path.of(path));
        webPagePosition = POSITION_TV;
        return "redirect:" + CommonHandler.CONFIG;
    }

    /*
     * Add new target path for movie
     * */
    @PostMapping(value = MOVIE_NEW_SOURCE_PATH)
    public String addPathMovie(@RequestParam String path, Model model)
            throws NoApiKeyException, ConfigurationException {
        propertiesService.addTargetPathMovie(Path.of(path));
        webPagePosition = POSITION_MOVIE;
        return "redirect:" + CommonHandler.CONFIG;
    }

    /*
     * Add new target path for tv
     * */
    @PostMapping(value = TV_NEW_SOURCE_PATH)
    public String addPathTv(@RequestParam String path, Model model)
            throws NoApiKeyException, ConfigurationException {
        propertiesService.addTargetPathTv(Path.of(path));
        webPagePosition = POSITION_TV;
        return "redirect:" + CommonHandler.CONFIG;
    }

    /*
     * Change current link path with option to move content to a new location
     * */
    @PostMapping(value = MOVIE_NEW_LINK_PATH)
    public String addLinksPath(LinksPathForm linksPathForm, Model model)
            throws NoApiKeyException, ConfigurationException {
        if (!linksPathForm.getLinksFilePath().isBlank()) {
            Path newLinksPath = Path.of(linksPathForm.getLinksFilePath());
            propertiesService.addLinksPathMovie(newLinksPath);
            //        if (linksPathForm.isMoveContent()) { // TODO
//            mediaLinksService.moveLinksToNewLocation(
//                    propertiesService.getLinksFolderMovie(),
//                    newLinksPath
//            );
//        }
        }
        webPagePosition = POSITION_MOVIE;
        return "redirect:" + CommonHandler.CONFIG;
    }

    /*
     * TODO read addidtional parameter from form and set value in the controller
     *  Then set it to the model so it gets set into invisible element as id
     * for some class, then let js script pick up this value and scroll page
     * to given id
     * */

    @PostMapping(value = TV_NEW_LINK_PATH)
    public String addLinkTv(LinksPathForm linksPathForm, Model model) throws NoApiKeyException, ConfigurationException {
        if (!linksPathForm.getLinksFilePath().isBlank()) {
            Path newLinksPath = Path.of(linksPathForm.getLinksFilePath());
            if (linksPathForm.isMoveContent()) {
                mediaLinksService.moveLinksToNewLocation(
                        propertiesService.getLinksFolderMovie(),
                        newLinksPath
                );
            }
            propertiesService.addLinksPathTv(newLinksPath);
        }
        webPagePosition = POSITION_TV;
        return "redirect:" + CommonHandler.CONFIG;
    }

    // Rename to something meaningful to let user know what action is being taken
    @PostMapping(value = CLEAR_FOLDERS_MOVIE)
    public String clearSelectedFolder(@RequestParam String path, Model model) throws IOException {
        webPagePosition = POSITION_MOVIE;
        if (mediaLinksService.findEmptyFolders(MediaType.MOVIE)) {
            model.addAttribute("folders_for_clearing", mediaLinksService.getFoldersForClearing());
            return "clear_folders_confirmation";
        }
        return "redirect:" + CommonHandler.CONFIG;
    }

    @GetMapping(value = CLEAR_FOLDERS_MOVIE_CONFIRM)
    public String confirmClearFoldersMovie(Model model) {
        mediaLinksService.persistRemoveEmptyFolders();
        return "redirect:" + CommonHandler.CONFIG;
    }

    @GetMapping(value = CLEAR_FOLDERS_MOVIE_ABORT)
    public String abortClearFoldersMovie(Model model) {
        mediaLinksService.abortFolderClearing();
        return "redirect:" + CommonHandler.CONFIG;
    }

    @GetMapping(value = BACKUP_DATABASE)
    public String backupDatabase(Model model) throws MissingFolderOrFileException, IOException {
        String dbBackupFileName = databaseMigrationService
                .backupDatabase(propertiesService.getDataFolder());
        webPagePosition = POSITION_BACKUP;
        return "redirect:" + CommonHandler.CONFIG;
    }
}

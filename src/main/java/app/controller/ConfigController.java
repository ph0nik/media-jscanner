package app.controller;

import model.form.LinksPathForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.MediaLinksService;
import service.PropertiesService;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import util.MediaFilter;

import java.nio.file.Path;
import java.util.List;

@Controller
//@RequestMapping(value = CommonHandler.CONFIG)
public class ConfigController {
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;
    private static final String MOVIE_NEW_LINK_PATH = "/config/movie-new-link-path/";
    private static final String MOVIE_NEW_SOURCE_PATH = "/config/movie-new-source-path/";
    private static final String MOVIE_DELETE_SOURCE_PATH = "/config/movie-delete-source-path/";
    private static final String TV_NEW_LINK_PATH = "/config/tv-new-link-path/";
    private static final String TV_NEW_SOURCE_PATH = "/config/tv-new-source-path/";
    private static final String TV_DELETE_SOURCE_PATH = "/config/tv-delete-source-path/";
    private static final String CLEAR_FOLDERS = "/config/clear-folders/";
    @ModelAttribute
    private void setConfigEndpoints(Model model) {
        model.addAttribute("movie_new_link", MOVIE_NEW_LINK_PATH);
        model.addAttribute("movie_new_source", MOVIE_NEW_SOURCE_PATH);
        model.addAttribute("movie_delete_target", MOVIE_DELETE_SOURCE_PATH);
        model.addAttribute("tv_new_link", TV_NEW_LINK_PATH);
        model.addAttribute("tv_new_target", TV_NEW_SOURCE_PATH);
        model.addAttribute("tv_delete_target", TV_DELETE_SOURCE_PATH);
        model.addAttribute("clear_folders", CLEAR_FOLDERS);
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
        Path movieLinksPath = propertiesService.getLinksFolderMovie();
        Path tvLinksPath = propertiesService.getLinksFolderTv();
        model.addAttribute("links_folder_movie", movieLinksPath);
        model.addAttribute("links_folder_tv", tvLinksPath);
        model.addAttribute("target_folder_movie", propertiesService.getTargetFolderListMovie());
        model.addAttribute("target_folder_tv", propertiesService.getTargetFolderListTv());
        model.addAttribute("links_path_form", new LinksPathForm());
        return "config";
    }

    // TODO check all incoming paths, if incorrect get proper exceptions and report back to user at the same option lvl
    /*
     * Delete selected target path for movie
     * */
    @PostMapping(value = MOVIE_DELETE_SOURCE_PATH)
    public String deletePathMovie(@RequestParam String path, Model model) throws ConfigurationException {
        propertiesService.removeTargetPathMovie(Path.of(path));
        return "redirect:" + CommonHandler.CONFIG;
    }

    /*
    * Delete selected target path for tv
    * */
    @PostMapping(value = TV_DELETE_SOURCE_PATH)
    public String deletePathTv(@RequestParam String path, Model model) throws ConfigurationException {
        propertiesService.removeTargetPathTv(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Add new target path for movie
    * */
    @PostMapping(value = MOVIE_NEW_SOURCE_PATH)
    public String addPathMovie(@RequestParam String path, Model model) throws NoApiKeyException, ConfigurationException {
        propertiesService.addTargetPathMovie(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Add new target path for tv
    * */
    @PostMapping(value = TV_NEW_SOURCE_PATH)
    public String addPathTv(@RequestParam String path, Model model) throws NoApiKeyException, ConfigurationException {
        propertiesService.addTargetPathTv(Path.of(path));
        return "redirect:/config";
    }

    /*
     * Change current link path with option to move content to a new location
     * */
    @PostMapping(value = MOVIE_NEW_LINK_PATH)
    public String addLinksPath(LinksPathForm linksPathForm, Model model) throws NoApiKeyException, ConfigurationException {
        Path newLinksPath = Path.of(linksPathForm.getLinksFilePath());
        if (linksPathForm.isMoveContent()) {
            mediaLinksService.moveLinksToNewLocation(propertiesService.getLinksFolderMovie(), newLinksPath);
        }
        propertiesService.setLinksPathMovie(newLinksPath);
        return "redirect:/config";
    }

    @PostMapping(value = TV_NEW_LINK_PATH)
    public String addLinkTv(LinksPathForm linksPathForm, Model model) throws NoApiKeyException, ConfigurationException {
        Path newLinksPath = Path.of(linksPathForm.getLinksFilePath());
        if (linksPathForm.isMoveContent()) {
            mediaLinksService.moveLinksToNewLocation(propertiesService.getLinksFolderMovie(), newLinksPath);
        }

        propertiesService.setLinksPathTv(newLinksPath);
        return "redirect:/config";
    }


    // Rename to something meaningful to let user know what action is being taken
    @PostMapping(value = CLEAR_FOLDERS)
    public String clearSelectedFolder(@RequestParam String path, Model model) {
        mediaLinksService.removeEmptyFolders(path);
        return "redirect:/config";
    }
}

package app.controller;

import model.MediaLink;
import model.form.LinksPathForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.FileService;
import service.MediaLinksService;
import service.PropertiesService;
import service.exceptions.ConfigurationException;
import service.exceptions.NoApiKeyException;
import util.MediaFilter;
import util.TrayMenu;

import java.nio.file.Path;
import java.util.List;

@Controller
public class TrackerController {
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private FileService fileService;
    @Autowired
    private TrayMenu trayMenu;
    private void initTracker() {
        trayMenu.createTray();
    }
    @Value("${go.config.movie.link}")
    private String movieNewLinkPath;
    @Value("${go.config.movie.target}")
    private String movieNewTargetPath;
    @Value("${go.config.movie.delete}")
    private String movieDeleteTargetPath;
    @Value("${go.config.tv.link}")
    private String tvNewLinkPath;
    @Value("${go.config.tv.target}")
    private String tvNewTargetPath;
    @Value("${go.config.tv.delete}")
    private String tvDeleteTargetPath;
    @Value("${go.config.clear}")
    private String clearFolders;
    private void setConfigEndpoints(Model model) {
        model.addAttribute("movie_new_link", movieNewLinkPath);
        model.addAttribute("movie_new_target", movieNewTargetPath);
        model.addAttribute("movie_delete_target", movieDeleteTargetPath);
        model.addAttribute("tv_new_link", tvNewLinkPath);
        model.addAttribute("tv_new_target", tvNewTargetPath);
        model.addAttribute("tv_delete_target", tvDeleteTargetPath);
        model.addAttribute("clear_folders", clearFolders);
    }


    @ModelAttribute("link_list")
    public List<MediaLink> getAllMediaLinks() {
        return mediaLinksService.getMediaLinks();
    }

    @ModelAttribute("media_ignored")
    public List<MediaLink> getAllIgnoredMedia() {
        return mediaLinksService.getMediaIgnoredList();
    }

    @ModelAttribute("user_paths")
    public boolean checkForUserProvidedPaths() {
        return propertiesService.checkUserPaths();
    }

    @ModelAttribute("extensions")
    public List<String> getCurrentExtensions() {
        return MediaFilter.getExtensions();
    }

    /*
    * Returns configuration panel
    * */
    @GetMapping("/config")
    public String configuration(Model model) {
        Path linksFolder = propertiesService.getLinksFolderMovie();
        Path tvLinksPath = propertiesService.getLinksFolderTv();
        model.addAttribute("links_folder_movie", linksFolder);
        model.addAttribute("links_folder_tv", tvLinksPath);
        model.addAttribute("target_folder_movie", propertiesService.getTargetFolderListMovie());
        model.addAttribute("target_folder_tv", propertiesService.getTargetFolderListTv());
        model.addAttribute("links_path_validated", fileService.validatePath(linksFolder.toString()));
        model.addAttribute("links_path_form", new LinksPathForm());
        return "config";
    }

    /*
     * Delete selected target path for movie
     * */
    @PostMapping("/delete-path-movie")
    public String deletePathMovie(@RequestParam String path, Model model) throws NoApiKeyException, ConfigurationException {
        propertiesService.removeTargetPathMovie(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Delete selected target path for tv
    * */
    @PostMapping("/delete-path-tv")
    public String deletePathTv(@RequestParam String path, Model model) throws NoApiKeyException, ConfigurationException {
        propertiesService.removeTargetPathTv(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Add new target path for movie
    * */
    @PostMapping("/add-target-movie")
    public String addPathMovie(@RequestParam String path, Model model) throws NoApiKeyException, ConfigurationException {
        propertiesService.addTargetPathMovie(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Add new target path for tv
    * */
    @PostMapping("/add-target-tv")
    public String addPathTv(@RequestParam String path, Model model) throws NoApiKeyException, ConfigurationException {
        propertiesService.addTargetPathTv(Path.of(path));
        return "redirect:/config";
    }

    /*
     * Change current link path with option to move content to a new location
     * */
    @PostMapping("/add-link-movie")
    public String addLinksPath(LinksPathForm linksPathForm, Model model) throws NoApiKeyException, ConfigurationException {
        Path newLinksPath = Path.of(linksPathForm.getLinksFilePath());
        if (linksPathForm.isMoveContent()) {
            mediaLinksService.moveLinksToNewLocation(propertiesService.getLinksFolderMovie(), newLinksPath);
        }
        propertiesService.setLinksPathMovie(newLinksPath);
        return "redirect:/config";
    }

    @PostMapping("/add-link-tv")
    public String addLinkTv(LinksPathForm linksPathForm, Model model) throws NoApiKeyException, ConfigurationException {
        Path newLinksPath = Path.of(linksPathForm.getLinksFilePath());
        if (linksPathForm.isMoveContent()) {
            mediaLinksService.moveLinksToNewLocation(propertiesService.getLinksFolderMovie(), newLinksPath);
        }

        propertiesService.setLinksPathTv(newLinksPath);
        return "redirect:/config";
    }


    @PostMapping("/clear-folders")
    public String clearSelectedFolder(@RequestParam String path, Model model) {
        mediaLinksService.removeEmptyFolders(path);
        return "redirect:/config";
    }
}

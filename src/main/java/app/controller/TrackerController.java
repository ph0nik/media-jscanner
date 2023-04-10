package app.controller;

import model.MediaLink;
import model.MediaQuery;
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
    private TrayMenu trayMenu;

//    @Autowired
//    private TrackerExecutor trackerExecutor;

    /*
     * Initialize file watcher at startup
     * */
//    @PostConstruct
    private void initTracker() {
        // Temporarily tracker turned off
//        trackerExecutor.startTracker();
        trayMenu.createTray();
    }

    @ModelAttribute("query_list")
    public List<MediaQuery> getAllMediaQueries() {
        return mediaLinksService.getMediaQueryList();
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
        model.addAttribute("links_path_validated", mediaLinksService.validatePath(linksFolder.toString()));
        model.addAttribute("links_path_form", new LinksPathForm());
        return "config";
    }

    /*
     * Delete selected target path for movie
     * */
    @PostMapping("/delete-path-movie")
    public String deletePathMovie(@RequestParam String path, Model model) {
        propertiesService.removeTargetPathMovie(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Delete selected target path for tv
    * */
    @PostMapping("/delete-path-tv")
    public String deletePathTv(@RequestParam String path, Model model) {
        propertiesService.removeTargetPathTv(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Add new target path for movie
    * */
    @PostMapping("/add-target-movie")
    public String addPathMovie(@RequestParam String path, Model model) {
        propertiesService.addTargetPathMovie(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Add new target path for tv
    * */
    @PostMapping("/add-target-tv")
    public String addPathTv(@RequestParam String path, Model model) {
        propertiesService.addTargetPathTv(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Change current link path with option to move content to a new location
    * */
    @PostMapping("/addlink")
    public String addLinksPath(LinksPathForm linksPathForm, Model model) {
        Path newLinksPath = Path.of(linksPathForm.getLinksFilePath());
        if (linksPathForm.isMoveContent()) {
            mediaLinksService.moveLinksToNewLocation(propertiesService.getLinksFolderMovie(), newLinksPath);
        }
        propertiesService.setLinksPath(newLinksPath);
        return "redirect:/config";
    }


    @PostMapping("/clear-folders")
    public String clearSelectedFolder(@RequestParam String path, Model model) {
        mediaLinksService.removeEmptyFolders(path);
        return "redirect:/config";
    }
}

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
import util.TrayMenu;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @PostConstruct
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

    /*
     * Reload tracker manually
     * */
//    @GetMapping("/reload")
//    public String reloadTracker() {
//        trackerExecutor.stopTracker();
//        trackerExecutor.startTracker();
//        return "redirect:/config";
//    }

    /*
    * Returns configuration panel
    * */
    @GetMapping("/config")
    public String configuration(Model model) {
        /*
         * Compares actual paths from properties file with paths injected into file watcher.
         * If they differ watcher needs to be restarted
         * */
//        boolean trackerPaths = trackerExecutor.compareTargetList(propertiesService.getTargetFolderList());
//        /*
//         * Returns true if file watcher is running
//         * */
//        boolean trackerStatus = trackerExecutor.trackerStatus();

        Path linksFolder = propertiesService.getLinksFolder();
        boolean linksPathValid = mediaLinksService.validatePath(linksFolder);

        List<Path> targetFolderList = propertiesService.getTargetFolderList();
        Map<Path, Boolean> pathsValidated = new HashMap<>();
        for (Path p : targetFolderList) {
            pathsValidated.put(p, mediaLinksService.validatePath(p));
        }
        boolean userLinksPath = propertiesService.isUserLinksPath();
        boolean userTargetPath = propertiesService.isUserTargetPath();

//        model.addAttribute("tracker_status", trackerStatus);
//        model.addAttribute("server_updated", trackerPaths);
        model.addAttribute("chk_user_target", userTargetPath);
        model.addAttribute("chk_user_links", userLinksPath);
        model.addAttribute("links_folder", linksFolder);
        model.addAttribute("target_folder_list", targetFolderList);
        model.addAttribute("target_path_validated", pathsValidated);
        model.addAttribute("links_path_validated", linksPathValid);
        model.addAttribute("links_path_form", new LinksPathForm());
        return "config";
    }

    /*
     * Delete target path from list
     * */
    @PostMapping("/deletepath")
    public String deletePath(@RequestParam String path, Model model) {
        propertiesService.removeTargetPath(Path.of(path));
        return "redirect:/config";
    }

    /*
    * Add new target path
    * */
    @PostMapping("/addtarget")
    public String addPath(@RequestParam String path, Model model) {
        propertiesService.setTargetPath(Path.of(path));
        // TODO path validation
        return "redirect:/config";
    }

    /*
    * Change current link path with option to move content to a new location
    * */
    @PostMapping("/addlink")
    public String addLinksPath(LinksPathForm linksPathForm, Model model) {
        Path newLinksPath = Path.of(linksPathForm.getLinksFilePath());
        if (linksPathForm.isMoveContent()) {
            mediaLinksService.moveLinksToNewLocation(propertiesService.getLinksFolder(), newLinksPath);
        }
        propertiesService.setLinksPath(newLinksPath);
        return "redirect:/config";
    }
}

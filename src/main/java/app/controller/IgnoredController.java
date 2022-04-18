package app.controller;

import dao.MediaTrackerDao;
import model.MediaIgnored;
import model.MediaLink;
import model.MediaQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import service.MediaLinksService;
import service.PropertiesService;

import java.util.List;

@Controller
public class IgnoredController {

    @Autowired
    private MediaTrackerDao mediaTrackerDao;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private MediaLinksService mediaLinksService;

    @ModelAttribute("query_list")
    public List<MediaQuery> getAllMediaQueries() {
        return mediaLinksService.getMediaQueryList();
    }

    @ModelAttribute("link_list")
    public List<MediaLink> getAllMediaLinks() {
        return mediaLinksService.getMediaLinks();
    }

    @ModelAttribute("media_ignored")
    public List<MediaIgnored> getAllIgnoredMedia() {
        return mediaTrackerDao.getAllMediaIgnored();
    }

    @ModelAttribute("user_paths")
    public boolean checkForUserProvidedPaths() {
        return propertiesService.checkUserPaths();
    }

    @GetMapping("/ignored")
    public String showIgnoredFiles(Model model) {
        return "ignored";
    }

    @PostMapping("/addignore/{id}")
    public String addToIgnoreList(@PathVariable("id") long id, Model model)  {
        mediaLinksService.ignoreMediaFile(id);
        return "redirect:/query";
    }

    @PostMapping("/removeignore/{id}")
    public String removeFromIgnroredList(@PathVariable("id") long id, Model model) {
        mediaLinksService.unIgnoreMedia(id);
        return "redirect:/ignored";
    }


}

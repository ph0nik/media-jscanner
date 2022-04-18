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
import service.AutoMatcherService;
import service.MediaLinksService;
import service.PropertiesService;

import java.util.List;

@Controller
public class StubsController {

    @Autowired
    private MediaTrackerDao mediaTrackerDao;

    @Autowired
    private MediaLinksService mediaLinksService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private AutoMatcherService autoMatcherService;

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

    @GetMapping("/stubs")
    public String showStubTab(Model model) {

        return "stubs";
    }

}

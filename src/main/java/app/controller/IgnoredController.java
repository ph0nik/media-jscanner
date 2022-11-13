package app.controller;

import model.MediaLink;
import model.MediaQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.MediaLinksService;
import service.MediaQueryService;
import service.PropertiesService;

import java.util.List;

@Controller
public class IgnoredController {

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private MediaLinksService mediaLinksService;

    @Autowired
    private MediaQueryService mediaQueryService;

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

    @GetMapping("/ignored")
    public String showIgnoredFiles(Model model) {
        return "ignored";
    }

    @PostMapping("/addignore/{id}")
    public String addToIgnoreList(@PathVariable("id") long id, @RequestParam String uuid, Model model)  {
        MediaQuery queryByUuid = mediaQueryService.getQueryByUuid(uuid);
        mediaLinksService.ignoreMediaFile(queryByUuid);
        return "redirect:/query";
    }

    @PostMapping("/removeignore/{id}")
    public String removeFromIgnroredList(@PathVariable("id") long id, Model model) {
        mediaLinksService.unIgnoreMedia(id);
        return "redirect:/ignored";
    }


}

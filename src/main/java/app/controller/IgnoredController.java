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
import java.util.UUID;

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

    @PostMapping("/add-ignore/{id}")
    public String addToIgnoreList(@PathVariable("id") long id, @RequestParam UUID uuid, Model model)  {
        mediaQueryService.setReferenceQuery(uuid);
        mediaQueryService.addQueryToProcess(mediaQueryService.getReferenceQuery());
        mediaLinksService.ignoreMediaFile();
        return "redirect:/";
    }

    @PostMapping("/remove-ignore/{id}")
    public String removeFromIgnroredList(@PathVariable("id") long id, Model model) {
        mediaLinksService.unIgnoreMedia(id);
        return "redirect:/ignored";
    }

    @PostMapping("/search-ignore/")
    public String searchWithGivenPhrase(@RequestParam String query, Model model) {

        return "ignored";
    }


}

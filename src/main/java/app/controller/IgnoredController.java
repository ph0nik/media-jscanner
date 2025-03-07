package app.controller;

import model.MediaLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.MediaLinksService;
import service.PropertiesService;
import service.query.MovieQueryService;

import java.util.List;

@Controller
public class IgnoredController {
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private MovieQueryService movieQueryService;
    @Value("${go.ignore.search}")
    private String ignoreSearch;
    @Value("${go.ignore.remove}")
    private String ignoreDelete;
    @Value("${go.link.clear}")
    private String clearLinks;
    @ModelAttribute
    private void setIgnoreEndpoints(Model model) {
        model.addAttribute("ignore_search", ignoreSearch);
        model.addAttribute("ignore_delete", ignoreDelete);
        model.addAttribute("link_clear", clearLinks);
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

    @GetMapping("${tab.ignored}")
    public String showIgnoredFiles(Model model) {
        return "ignored";
    }

    @PostMapping("${go.ignore.remove}")
    public String removeFromIgnoredList(@RequestParam("id") long id, Model model) {
        mediaLinksService.unIgnoreMedia(id);
        return "redirect:/ignored";
    }

    @PostMapping("${go.ignore.search}")
    public String searchWithGivenPhrase(@RequestParam String query, Model model) {

        return "ignored";
    }


}

package app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.MediaLinksService;
import service.PropertiesService;
import service.query.MovieQueryService;

@Controller
public class IgnoredController {
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private MovieQueryService movieQueryService;
    private static final String SEARCH_IGNORED = "/search-ignore/";
    private static final String DELETE_IGNORED = "/remove-ignore/";

    @ModelAttribute
    private void setIgnoreEndpoints(Model model) {
        model.addAttribute("ignore_search", SEARCH_IGNORED);
        model.addAttribute("ignore_delete", DELETE_IGNORED);
        model.addAttribute("link_clear", LinksController.CLEAR_LINKS);
    }

    @GetMapping(value = CommonHandler.IGNORED)
    public String showIgnoredFiles(Model model) {
        return "ignored";
    }

    @PostMapping(value = DELETE_IGNORED)
    public String removeFromIgnoredList(@RequestParam("id") long id, Model model) {
        mediaLinksService.unIgnoreMedia(id);
        return "redirect:" + CommonHandler.IGNORED;
    }

    @PostMapping("${go.ignore.search}")
    public String searchWithGivenPhrase(@RequestParam String query, Model model) {

        return "ignored";
    }


}

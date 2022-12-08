package app.controller;

import model.LinkCreationResult;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.form.WebSearchResultForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import service.ErrorNotificationService;
import service.MediaLinksService;
import service.PropertiesService;
import util.MediaIdentity;

import java.util.Comparator;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class LinksController {

    @Autowired
    private MediaLinksService mediaLinksService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private ErrorNotificationService errorNotificationService;

    @ModelAttribute("error")
    public String getCurrentResult() {
        return errorNotificationService.getCurrentResult();
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
     * Create new link with query id and query result object.
     * */
    @PostMapping("/newlink")
    public String newLink(WebSearchResultForm webSearchResultForm,
                          BindingResult bindingResult,
                          Model model) {
        System.out.println(webSearchResultForm);
        QueryResult qr = new QueryResult();
        qr.setId(webSearchResultForm.getId());
        qr.setImdbId(webSearchResultForm.getImdbId());
        qr.setDescription(webSearchResultForm.getDescription());
        qr.setTitle(webSearchResultForm.getTitle());
        qr.setTheMovieDbId(webSearchResultForm.getTheMovieDbId());
        qr.setOriginalPath(webSearchResultForm.getOriginalPath());
        qr.setUrl(webSearchResultForm.getUrl());
        MediaIdentity mediaIdentity = (webSearchResultForm.getImdbId().isEmpty()) ? MediaIdentity.TMDB : MediaIdentity.IMDB;
        // TODO pass exceptions info to user
        // get grouped queries that are marked only as part of the same title
        // TODO make this function for collection of media queries
        List<LinkCreationResult> linkCreationResults = mediaLinksService.createFileLink(qr, mediaIdentity);
        // TODO implement list of results
        linkCreationResults.forEach(lcr -> errorNotificationService.setLinkCreationResult(lcr));
        return "redirect:/query";
    }

    @GetMapping("/newlink")
    public String newLinkGet() {
        return "redirect:/links";
    }

    // TODO add pagination and search
    /*
     * Show all existing symlinks.
     * */
    @RequestMapping(value = "/links", method = GET)
    public String linksSorted(@RequestParam(value = "sort", required = false) String sort, Model model) {
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();
        /*
         * Optional request parameter is being evaluated and list is sorted
         * accordingly. If no argument is given sorting falls back to default.
         * */
        if (sort == null || sort.isEmpty()) sort = "target";
        if (sort.equals("target")) {
            Comparator<MediaLink> comparator = Comparator.comparing(MediaLink::getOriginalPath);
            allMediaLinks.sort(comparator);
        }
        if (sort.equals("link")) {
            Comparator<MediaLink> comparator = Comparator.comparing(MediaLink::getLinkPath);
            allMediaLinks.sort(comparator);
        }

//        boolean userPathsProvided = propertiesService.checkUserPaths();

        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
//        model.addAttribute("user_paths", userPathsProvided);
        return "links";
    }

    // TODO search link
    @PostMapping("/search-link/")
    public String searchLink(@RequestParam("query") String query, Model model) {
        return "";
    }


    @PostMapping("/removelink/{id}")
    public String newLink(@PathVariable("id") long id, Model model) {
        mediaLinksService.moveBackToQueue(id);
        return "redirect:/";
    }

    @PostMapping("/delete-original/{id}")
    public String deleteOriginal(@PathVariable("id") long id, Model model) {
        mediaLinksService.deleteOriginalFile(id);
        return "redirect:/links";
    }

    @PostMapping("/restore-original/{id}")
    public String restoreOriginal(@PathVariable("id") long id, Model model) {
        mediaLinksService.restoreOriginalFile(id);
        return "redirect:/links";
    }
}

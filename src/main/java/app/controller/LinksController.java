package app.controller;

import model.MediaIgnored;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.form.WebSearchResultForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
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

    @ModelAttribute("media_ignored")
    public List<MediaIgnored> getAllIgnoredMedia() {
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
        boolean userPathsProvided = propertiesService.checkUserPaths();

        QueryResult qr = new QueryResult();
        qr.setId(webSearchResultForm.getId());
        qr.setImdbId(webSearchResultForm.getImdbId());
        qr.setDescription(webSearchResultForm.getDescription());
        qr.setTitle(webSearchResultForm.getTitle());
        qr.setTheMovieDbId(webSearchResultForm.getTheMovieDbId());
        qr.setFilePath(webSearchResultForm.getFilePath());
        qr.setUrl(webSearchResultForm.getUrl());
        MediaIdentity mediaIdentity = (webSearchResultForm.getImdbId().isEmpty()) ? MediaIdentity.TMDB : MediaIdentity.IMDB;
        mediaLinksService.createSymLink(qr, mediaIdentity, webSearchResultForm.getMediaType());
        return "redirect:/query";
    }

    @GetMapping("/newlink")
    public String newLinkGet() {
        return "redirect:/links";
    }

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
            Comparator<MediaLink> comparator = Comparator.comparing(MediaLink::getTargetPath);
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

    @PostMapping("/removelink/{id}")
    public String newLink(@PathVariable("id") long id, Model model) {
        MediaQuery backToQueue = mediaLinksService.moveBackToQueue(id);
        return "redirect:/query";
    }
}

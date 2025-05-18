package app.controller;

import model.MediaLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.MediaLinksService;
import service.PropertiesService;
import service.SortBy;
import service.query.MediaQueryService;

import java.io.IOException;
import java.util.Optional;

@Controller
public class LinksController {

    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    @Qualifier("movieQuery")
    private MediaQueryService movieQueryService;
    @Autowired
    @Qualifier("tvQuery")
    private MediaQueryService tvQueryService;
    private int sessionPageSize = 25;
    private static final String CREATE_NEW_LINK = "/newlink";
    public static final String CLEAR_LINKS = "/clear-links/";
    private static final String SEARCH_LINKS = "/search-link/";
    private static final String REMOVE_LINK = "/remove-link/";
    private static final String DELETE_SOURCE_FILE = "/delete-original/";
    private static final String RESTORE_SOURCE_FILE = "/restore-original/";

    //    private static final String LINKS = "/links";
    @ModelAttribute
    private void setMenuLinks(Model model) {
//        model.addAttribute("link_new", CREATE_NEW_LINK);
        model.addAttribute("link_clear", CLEAR_LINKS);
        model.addAttribute("link_search", SEARCH_LINKS);
        model.addAttribute("link_remove", REMOVE_LINK);
        model.addAttribute("link_delete_org", DELETE_SOURCE_FILE);
        model.addAttribute("link_restore_org", RESTORE_SOURCE_FILE);
        model.addAttribute("current_menu", 2);
    }

    /*
     * Show all existing symlinks.
     * */
    @GetMapping(value = CommonHandler.LINKS)
    public String linksSorted(@RequestParam(value = "sort", required = false) String sort,
                              @RequestParam("page") Optional<Integer> page,
                              @RequestParam("size") Optional<Integer> size,
                              Model model) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(sessionPageSize);
        sessionPageSize = pageSize;
        int min = currentPage * pageSize - pageSize + 1;
        int max = currentPage * pageSize;
        /*
         * Optional request parameter is being evaluated and list is sorted
         * accordingly. If no argument is given sorting falls back to default.
         * */
        SortBy sortBy;
        if (sort == null || sort.equals("link")) sortBy = SortBy.LINK_PATH;
        else if (sort.equals("target")) sortBy = SortBy.SOURCE_PATH;
        else sortBy = SortBy.DELETED_SOURCE_PATH;

        Page<MediaLink> paginatedLinks = mediaLinksService.getPageableLinksWithSorting(
                PageRequest.of(currentPage - 1, pageSize),
                sortBy
        );

//        Comparator<MediaLink> comparator = (sort != null && sort.equals("link"))
//                ? Comparator.comparing(MediaLink::getLinkPath)
//                : Comparator.comparing(MediaLink::getOriginalPath);
//        List<MediaLink> allMediaLinks = mediaLinksService
//                .getMediaLinks()
//                .stream()
//                .sorted(comparator)
//                .collect(Collectors.toList());
//        Page<MediaLink> paginatedLinks = mediaLinksService
//                .getPageableLinks(
//                        PageRequest.of(currentPage - 1, pageSize),
//                        allMediaLinks
//                );
        // TODO update all dead files on reload
        // TODO show only missing files
        model.addAttribute("page", paginatedLinks);
        model.addAttribute("page_min", min);
        model.addAttribute("page_max", max);
        return "links";
    }

    @GetMapping(value = CLEAR_LINKS)
    public String clearLinks(Model model) {
        mediaLinksService.clearInvalidIgnoreAndLinks();
        return "redirect:" + CommonHandler.LINKS;
    }

    @PostMapping(value = REMOVE_LINK)
    public String removeLink(@RequestParam("id") long id, Model model) throws IOException {
        mediaLinksService.moveBackToQueue(id);
        return "redirect:" + CommonHandler.SCAN_FOR_MEDIA;
    }

    @PostMapping(value = DELETE_SOURCE_FILE)
    public String deleteOriginal(@RequestParam("id") long id, Model model) {
        mediaLinksService.deleteOriginalFile(id);
        return "redirect:" + CommonHandler.LINKS;
    }

    @PostMapping(value = RESTORE_SOURCE_FILE)
    public String restoreOriginal(@RequestParam("id") long id, Model model) throws IOException {
        mediaLinksService.restoreOriginalFile(id);
        return "redirect:" + CommonHandler.LINKS;
    }
}

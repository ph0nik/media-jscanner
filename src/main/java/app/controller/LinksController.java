package app.controller;

import model.MediaLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.ErrorNotificationService;
import service.MediaLinksService;
import service.PropertiesService;
import service.query.MediaQueryService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class LinksController {

    @Autowired
    private Router router;
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
    @Autowired
    private ErrorNotificationService errorNotificationService;
    private int sessionPageSize = 25;
    @Value("${go.link.new}")
    private String newLink;
    @Value("${go.link.clear}")
    private String clearLinks;
    @Value("${go.link.search}")
    private String searchLinks;
    @Value("${go.link.remove}")
    private String removeLink;
    @Value("${go.link.deloriginal}")
    private String deleteOriginal;
    @Value("${go.link.restore}")
    private String restoreOriginal;
    @ModelAttribute
    private void setMenuLinks(Model model) {
        model.addAttribute("link_new", newLink);
        model.addAttribute("link_clear", clearLinks);
        model.addAttribute("link_search", searchLinks);
        model.addAttribute("link_remove", removeLink);
        model.addAttribute("link_delete_org", deleteOriginal);
        model.addAttribute("link_restore_org", restoreOriginal);
    }

    @ModelAttribute("error")
    public String getCurrentResult() {
        return errorNotificationService.getCurrentResult();
    }

    @ModelAttribute("link_list")
    public List<MediaLink> getAllMediaLinks() {
        return mediaLinksService.getMediaLinks();
    }

    /*
     * Show all existing symlinks.
     * */
    @GetMapping("${tab.links}")
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
        Comparator<MediaLink> comparator = (sort != null && sort.equals("link"))
                ? Comparator.comparing(MediaLink::getLinkPath)
                : Comparator.comparing(MediaLink::getOriginalPath);
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks().stream().sorted(comparator).collect(Collectors.toList());
        Page<MediaLink> paginatedLinks = mediaLinksService.getPageableLinks(PageRequest.of(currentPage - 1, pageSize), allMediaLinks);
        // TODO update all dead files on reload
        // TODO show only missing files
        model.addAttribute("page", paginatedLinks);
        model.addAttribute("page_min", min);
        model.addAttribute("page_max", max);
        return "links";
    }

    @GetMapping("${go.link.clear}")
    public String clearLinks(Model model) {
        mediaLinksService.clearInvalidIgnoreAndLinks();
        return "redirect:/links";
    }

    @PostMapping("${go.link.search}")
    public String searchLink(@RequestParam("search") String search, Model model) {
        int min = 1;
        int max = sessionPageSize;
        Page<MediaLink> paginatedLinks = mediaLinksService.getPageableLinks(PageRequest.of(0, sessionPageSize), mediaLinksService.searchMediaLinks(search));
        model.addAttribute("page", paginatedLinks);
        model.addAttribute("page_min", min);
        model.addAttribute("page_max", max);
        return "links";
    }

    @PostMapping("${go.link.remove}")
    public String newLink(@RequestParam("id") long id, Model model) {
        mediaLinksService.moveBackToQueue(id);
        return "redirect:/scan";
    }

    @PostMapping("${go.link.deloriginal}{id}")
    public String deleteOriginal(@PathVariable("id") long id, Model model) {
        mediaLinksService.deleteOriginalFile(id);
        return "redirect:/links";
    }

    @PostMapping("${go.link.restore}{id}")
    public String restoreOriginal(@PathVariable("id") long id, Model model) {
        mediaLinksService.restoreOriginalFile(id);
        return "redirect:/links";
    }
}

package app.controller;

import model.MediaLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.MediaLinksService;
import service.PropertiesService;
import service.query.MovieQueryService;

import java.util.Optional;

@Controller
public class IgnoredController {
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private MovieQueryService movieQueryService;
    private int sessionPageSize = 25;
    private static final String DELETE_IGNORED = "/remove-ignore/";

    @ModelAttribute
    private void setIgnoreEndpoints(Model model) {
        model.addAttribute("ignore_delete", DELETE_IGNORED);
        model.addAttribute("find_invalid_media", LinksController.FIND_INVALID_MEDIA);
        model.addAttribute("current_menu", 3);
    }

    @GetMapping(value = CommonHandler.IGNORED)
    public String showIgnoredFiles(@RequestParam("page") Optional<Integer> page,
                                   @RequestParam("size") Optional<Integer> size,
                                   Model model) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(sessionPageSize);
        sessionPageSize = pageSize;
        int min = currentPage * pageSize - pageSize + 1;
        int tempMax = currentPage * pageSize;
        int max = Math.min(currentPage * pageSize, mediaLinksService.getMediaIgnoredList().size());
//        int max = currentPage * pageSize;
        Page<MediaLink> paginatedLinks = mediaLinksService.getPageableLinks(
                PageRequest.of(currentPage - 1, pageSize),
                mediaLinksService.getMediaIgnoredList());
        model.addAttribute("page", paginatedLinks);
        model.addAttribute("page_min", min);
        model.addAttribute("page_max", max);
        return "ignored";
    }

    @PostMapping(value = DELETE_IGNORED)
    public String removeFromIgnoredList(@RequestParam("id") long id, Model model) {
        mediaLinksService.undoIgnoreMedia(id);
        return "redirect:" + CommonHandler.IGNORED;
    }


}

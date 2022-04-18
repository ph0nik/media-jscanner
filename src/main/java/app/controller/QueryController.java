package app.controller;

import dao.MediaTrackerDao;
import model.*;
import model.form.WebSearchResultForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.AutoMatcherService;
import service.MediaLinksService;
import service.PropertiesService;
import util.MediaIdentity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

@Controller
public class QueryController {

    @Autowired
    private MediaTrackerDao mediaTrackerDao;

    @Autowired
    private MediaLinksService mediaLinksService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private AutoMatcherService autoMatcherService;

    private Future<List<MediaLink>> future;

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

    /*
     * Show elements awaiting in the queue and let user select
     * file to process or let auto matcher guess correct movies.
     * */
    @GetMapping("/query")
    public String queryList(@RequestParam("page") Optional<Integer> page,
                            @RequestParam("size") Optional<Integer> size,
                            Model model) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(20);

        Page<MediaQuery> paginatedQueries = mediaLinksService.findPaginatedQueries(PageRequest.of(currentPage - 1, pageSize));
//        Get Auto Matcher status
        boolean autoMatcherStatus = future == null || future.isDone();
        model.addAttribute("page", paginatedQueries);
        model.addAttribute("future", autoMatcherStatus);
        return "query_list";
    }

    /*
     * For selected file perform online search for matching titles.
     * Present returned results and prompt user to select which title
     * will be used to create symlink.
     * */
    @PostMapping("/selectquery/{id}")
    public String selectQuery(@PathVariable("id") long id, @RequestParam String custom, Model model) {
        // get query by id from db
        MediaQuery queryById = mediaTrackerDao.getQueryById(id);
        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery(custom, id, MediaIdentity.IMDB);

        model.addAttribute("result_list", queryResults);
        model.addAttribute("query", queryById);
        model.addAttribute("request_form", new WebSearchResultForm());
        return "result_selection";
    }

    /*
     * GET mapping as a protection measure in case user reloads page
     * */
    @GetMapping(value = {"/selectquery", "/selectquery/{id}"})
    public String selectQueryGet(@PathVariable(value = "id", required = false) Long id, Model model) {
        LastRequest latestMediaQuery = mediaLinksService.getLatestMediaQuery();
        if (latestMediaQuery == null) return "redirect:/query";
        MediaQuery queryById = mediaTrackerDao.getQueryById(latestMediaQuery.getLastId());

        model.addAttribute("result_list", latestMediaQuery.getLastRequest());
        model.addAttribute("request_form", new WebSearchResultForm());
        model.addAttribute("query", queryById);
        return "result_selection";
    }

    @GetMapping("/auto")
    public String autoMatch() {
        future = autoMatcherService.autoMatchFilesWithFuture();
        return "redirect:/query";
    }
}

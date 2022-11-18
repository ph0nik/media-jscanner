package app.controller;

import model.LastRequest;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.form.WebSearchResultForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import service.*;
import util.MediaIdentity;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

@Controller
//@RequestMapping(value = "/jscanner")
@Validated
public class QueryController {

//    @Autowired
//    @Qualifier("hibernate")
//    private MediaTrackerDao mediaTrackerDao;

    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private AutoMatcherService autoMatcherService;
    @Autowired
    private MediaQueryService mediaQueryService;
    @Autowired
    private ErrorNotificationService errorNotificationService;

    @ModelAttribute("error")
    public String getCurrentResult() {
        return errorNotificationService.getCurrentResult();
    }

    private Future<List<MediaLink>> future;
    private int sessionPageSize = 25;

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

    @GetMapping("/query")
    public String redirectToQuery() {
        return "redirect:/";
    }

    /*
     * Show elements awaiting in the queue and let user select
     * file to process or let auto matcher guess correct movies.
     * */
    @GetMapping("/")
    public String queryList(@RequestParam("page") Optional<Integer> page,
                            @RequestParam("size") Optional<Integer> size,
                            Model model) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(sessionPageSize);
        sessionPageSize = pageSize;
        int min = currentPage * pageSize - pageSize + 1;
        int max = currentPage * pageSize;

        Page<MediaQuery> paginatedQueries = mediaLinksService.findPaginatedQueries(PageRequest.of(currentPage - 1, pageSize), mediaLinksService.getMediaQueryList());

//        Get Auto Matcher status
        // 1 * 20 - max, min - 1 * 20 - 20 + 1
        // 2 * 20 - max, min - 2 * 20 - 20 + 1
        // 1 – 25 of 106
        // (currentPage + 1) * pageSize - pageSize + 1 "-" (currentPage + 1) * pageSize of queryList.size
        boolean autoMatcherStatus = future == null || future.isDone();
        model.addAttribute("page", paginatedQueries);
        model.addAttribute("future", autoMatcherStatus);
        model.addAttribute("page_min", min);
        model.addAttribute("page_max", max);
        return "query_list";
    }

    @PostMapping("/search-query/")
    public String searchQuery(@RequestParam("search") String search,
                              Model model) {
        int currentPage = 1;
        int pageSize = sessionPageSize;
        sessionPageSize = pageSize;
        int min = currentPage * pageSize - pageSize + 1;
        int max = currentPage * pageSize;
        List<MediaQuery> mediaQueries = mediaQueryService.searchQuery(search);
        Page<MediaQuery> paginatedQueries = mediaLinksService.findPaginatedQueries(PageRequest.of(currentPage - 1, pageSize), mediaQueries);
        boolean autoMatcherStatus = future == null || future.isDone();
        model.addAttribute("page", paginatedQueries);
        model.addAttribute("future", autoMatcherStatus);
        model.addAttribute("page_min", min);
        model.addAttribute("page_max", max);
        return "query_list";
    }

    /*
     * For selected file perform online search for matching titles.
     * Present returned results and prompt user to select which title
     * will be used to create symlink.
     * */
    @PostMapping("/selectquery/{id}")
    public String selectQuery(@PathVariable("id") Long id, @RequestParam String custom,
                              @RequestParam UUID uuid, Model model) {
        // get query by id from db
//        MediaQuery queryById = mediaTrackerDao.getQueryById(id);
//        MediaQuery queryById = mediaQueryService.getQueryById(id);
        MediaQuery queryByUuid = mediaQueryService.getQueryByUuid(uuid.toString());

        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery(custom, queryByUuid, MediaIdentity.IMDB);

        model.addAttribute("result_list", queryResults);
        model.addAttribute("query", queryByUuid);
        model.addAttribute("request_form", new WebSearchResultForm());
        return "result_selection";
    }

    @PostMapping("/searchwithyear/{id}")
    public String searchTmdbWithYear(@PathVariable("id") Long id, @RequestParam String custom,
                                     @RequestParam String uuid, @RequestParam Optional<Integer> year, Model model) {
        MediaQuery queryByUuid = mediaQueryService.getQueryByUuid(uuid);
        List<QueryResult> queryResults = mediaLinksService.searchTmdbWithTitleAndYear(custom, queryByUuid, MediaIdentity.IMDB, year.orElse(1000));

        model.addAttribute("result_list", queryResults);
        model.addAttribute("query", queryByUuid);
        model.addAttribute("request_form", new WebSearchResultForm());
        return "result_selection";
    }

    /*
     * GET mapping as a protection measure in case user reloads page
     * */
    @GetMapping(value = {"/selectquery", "/selectquery/{id}", "/searchwithyear/{id}"})
    public String selectQueryGet(@PathVariable(value = "id", required = false) Long id, Model model) {
        LastRequest latestMediaQuery = mediaLinksService.getLatestMediaQueryRequest();
        if (latestMediaQuery == null) return "redirect:/";
//        MediaQuery queryById = mediaTrackerDao.getQueryById(latestMediaQuery.getLastId());
        MediaQuery lastMediaQuery = latestMediaQuery.getLastMediaQuery();

        model.addAttribute("result_list", latestMediaQuery.getLastRequest());
        model.addAttribute("request_form", new WebSearchResultForm());
        model.addAttribute("query", lastMediaQuery);
        return "result_selection";
    }

    @GetMapping("/scan")
    public String scanFolders() {
        try {
            mediaQueryService.scanForNewMediaQueries(propertiesService.getTargetFolderList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/";
    }

    @GetMapping("/auto")
    public String autoMatch() {
        future = autoMatcherService.autoMatchFilesWithFuture();
        return "redirect:/";
    }
}

package app.controller;

import dao.MediaTrackerDao;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.form.WebSearchResultForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import runner.TrackerExecutor;
import service.AutoMatcherService;
import service.MediaLinksService;
import service.PropertiesService;
import util.MediaIdentity;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Future;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class SimpleController {

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private MediaTrackerDao mediaTrackerDao;

    @Autowired
    private MediaLinksService mediaLinksService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private TrackerExecutor trackerExecutor;

    @Autowired
    private AutoMatcherService autoMatcherService;

    private Future<Boolean> future;

    /*
    * Testing starting page
    * */
    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("appName", appName);
        return "home";
    }

    @GetMapping("/jscanner")
    public String startingPoint() {
        return "redirect:/query";
    }

    /*
    * Initialize file watcher at startup
    * */
    @PostConstruct
    private void initTracker() {
        trackerExecutor.startTracker();
    }

    /*
    * Reload tracker manually
    * */
    @GetMapping("/reload")
    public String reloadTracker() {
        trackerExecutor.stopTracker();
        trackerExecutor.startTracker();

        return "redirect:/config";
    }

    /*
     * Show elements awaiting in the queue and let user select
     * file to process or let auto matcher guess correct movies.
     * */
//    @GetMapping("/query")
//    public String queryList(Model model) {
//        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
//        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();
//
//        boolean userPathsProvided = propertiesService.checkUserPaths();
////        Get Auto Matcher status
//        boolean autoMatcherStatus = future == null || future.isDone();
//
//        // TODO pagination
//        model.addAttribute("query_list", allMediaQueries);
//        model.addAttribute("link_list", allMediaLinks);
//        model.addAttribute("user_paths", userPathsProvided);
//        model.addAttribute("passquery", new MediaQuery());
//        model.addAttribute("future", autoMatcherStatus);
//        return "query_list";
//    }

    @GetMapping("/query")
    public String queryList(@RequestParam("page") Optional<Integer> page,
                            @RequestParam("size") Optional<Integer> size,
                            Model model) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(20);

        Page<MediaQuery> paginatedQueries = mediaLinksService.findPaginatedQueries(PageRequest.of(currentPage - 1, pageSize));

        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();

        boolean userPathsProvided = propertiesService.checkUserPaths();
//        Get Auto Matcher status
        boolean autoMatcherStatus = future == null || future.isDone();

        // TODO pagination
        model.addAttribute("page", paginatedQueries);
        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
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
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();
        boolean userPathsProvided = propertiesService.checkUserPaths();
        // get query by id from db
        MediaQuery queryById = mediaTrackerDao.getQueryById(id);
        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery(custom, id, MediaIdentity.IMDB);

        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
        model.addAttribute("result_list", queryResults);
//        model.addAttribute("selection", new QueryResult());
        model.addAttribute("query", queryById);
        model.addAttribute("request_form", new WebSearchResultForm());
        return "result_selection";
    }

    /*
    * GET mapping as a protection measure in case user reloads page
    * */
    @GetMapping(value = {"/selectquery", "/selectquery/{id}"})
    public String selectQueryGet(@PathVariable(value = "id", required = false) Long id, Model model) {
        // TODO add file path id to query result to avoid double call to db
        List<QueryResult> latestMediaQuery = mediaLinksService.getLatestMediaQuery();
        if (latestMediaQuery == null) return "redirect:/query";
        String filePath = latestMediaQuery.get(0).getFilePath();
        // TODO remove dao reference
        MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(filePath);
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();
        boolean userPathsProvided = propertiesService.checkUserPaths();

        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
        model.addAttribute("result_list", latestMediaQuery);
        model.addAttribute("request_form", new WebSearchResultForm());
//        model.addAttribute("selection", new QueryResult());
        model.addAttribute("query", queryByFilePath);

        return "result_selection";
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
        System.out.println("create link with: " + qr);

        mediaLinksService.createSymLink(qr, MediaIdentity.IMDB, webSearchResultForm.getMediaType());
        return "redirect:/query";
    }

    @GetMapping("/newlink")
    public String newLinkGet() {
        return "redirect:/links";
    }

    @GetMapping("/auto")
    public String autoMatch() {
        future = autoMatcherService.autoMatchFilesWithFuture();
        return "redirect:/query";
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

        boolean userPathsProvided = propertiesService.checkUserPaths();

        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
        return "links";
    }

    @PostMapping("/removelink/{id}")
    public String newLink(@PathVariable("id") long id, Model model) {
//        MediaLink linkById = mediaTrackerDao.getLinkById(id);
        MediaQuery backToQueue = mediaLinksService.moveBackToQueue(id);
//        mediaTrackerDao.addQueryToQueue(backToQueue);
        return "redirect:/query";
    }

    //TODO if links path changes ask user if want to delete current folder, move files to new location or abort
    @GetMapping("/config")
    public String configuration(Model model) {
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();

        /*
        * Compares actual paths from properties file with paths injected into file watcher.
        * If they differ watcher needs to be restarted
        * */
        boolean trackerPaths = trackerExecutor.compareTargetList(propertiesService.getTargetFolderList());
        /*
        * Returns true if file watcher is running
        * */
        boolean trackerStatus = trackerExecutor.trackerStatus();

        Path linksFolder = propertiesService.getLinksFolder();
        boolean linksPathValid = mediaLinksService.validatePath(linksFolder);

        List<Path> targetFolderList = propertiesService.getTargetFolderList();
        Map<Path, Boolean> pathsValidated = new HashMap<>();
        for (Path p : targetFolderList) {
            pathsValidated.put(p, mediaLinksService.validatePath(p));
        }
        System.out.println(pathsValidated.get(targetFolderList.get(0)));
        boolean userPathsProvided = propertiesService.checkUserPaths();
        boolean userLinksPath = propertiesService.isUserLinksPath();
        boolean userTargetPath = propertiesService.isUserTargetPath();

        model.addAttribute("tracker_status", trackerStatus);
        model.addAttribute("server_updated", trackerPaths);
        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
        model.addAttribute("chk_user_target", userTargetPath);
        model.addAttribute("chk_user_links", userLinksPath);
        model.addAttribute("links_folder", linksFolder);
        model.addAttribute("target_folder_list", targetFolderList);
        model.addAttribute("target_path_validated", pathsValidated);
        model.addAttribute("links_path_validated", linksPathValid);
        return "config";
    }

    /*
    * Delete target path from list
    * */
    @PostMapping("/deletepath")
    public String deletePath(@RequestParam String path, Model model) {
        propertiesService.removeTargetPath(Path.of(path));
        System.out.println("delete " + path);
        return "redirect:/config";
    }

    //TODO report of falls back on default paths
    @PostMapping("/addtarget")
    public String addPath(@RequestParam String path, Model model) {
        propertiesService.setTargetPath(Path.of(path));
        System.out.println("add target " + path);
        return "redirect:/config";
    }

    @PostMapping("/addlink")
    public String addLinksPath(@RequestParam String linkpath, Model model) {
        propertiesService.setLinksPath(Path.of(linkpath));
        System.out.println("add link" + linkpath);
        return "redirect:/config";
    }

}

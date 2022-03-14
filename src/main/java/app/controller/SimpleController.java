package app.controller;

import dao.MediaTrackerDao;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import runner.TrackerExecutor;
import service.MediaLinksService;
import service.SymLinkProperties;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.nio.file.Path;
import java.util.List;

@Controller
public class SimpleController {

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private MediaTrackerDao mediaTrackerDao;

    @Autowired
    private MediaLinksService mediaLinksService;

    @Autowired
    private SymLinkProperties symLinkProperties;

    @Autowired
    private TrackerExecutor trackerExecutor;

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("appName", appName);
        return "home";
    }

    @PostConstruct
    private void initTracker() {
        trackerExecutor.startTracker();
    }

    @GetMapping("/tracker_start")
    public String startTrackerManually() {
        if (!trackerExecutor.trackerStatus()) trackerExecutor.startTracker();
        return "temp";
    }

    @GetMapping("/tracker_stop")
    public String stopTrackerManually() {
        if (trackerExecutor.trackerStatus()) trackerExecutor.stopTracker();
        return "temp";
    }

    @GetMapping("/tracker_status")
    public String checkTracker() {
        System.out.println(trackerExecutor.trackerStatus());
        return "temp";
    }

    /*
     * Show elements awaiting in the queue and let user select
     * file to process
     * */
    @GetMapping("/query")
    public String queryList(Model model) {
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
//        List<MediaQuery> allMediaQueries = mediaTrackerDao.getAllMediaQueries();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();
//        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        boolean userPathsProvided = symLinkProperties.checkUserPaths();
        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
        model.addAttribute("passquery", new MediaQuery());
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
        boolean userPathsProvided = symLinkProperties.checkUserPaths();
        // get query by id from db
        MediaQuery queryById = mediaTrackerDao.getQueryById(id);
        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery(custom, queryById);
        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
        model.addAttribute("result_list", queryResults);
        model.addAttribute("selection", new QueryResult());
        model.addAttribute("query", queryById);
        return "result_selection";
    }

    /*
    * Redirect as a protection measure in case user reloads page
    * */
    @GetMapping(value = {"/selectquery", "/selectquery/{id}"})
    public String selectQueryGet(@PathVariable(value = "id", required = false) Long id, Model model) {
        List<QueryResult> latestMediaQuery = mediaLinksService.getLatestMediaQuery();
        if (latestMediaQuery == null) return "redirect:/query";
        String filePath = latestMediaQuery.get(0).getFilePath();
        MediaQuery queryByFilePath = mediaTrackerDao.findQueryByFilePath(filePath);
//        MediaQuery queryById = mediaTrackerDao.getQueryById(id);
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();
        boolean userPathsProvided = symLinkProperties.checkUserPaths();
        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
        model.addAttribute("result_list", latestMediaQuery);
        model.addAttribute("selection", new QueryResult());
        model.addAttribute("query", queryByFilePath);
        return "result_selection";
    }

    /*
    * Create new link with query id and query result object.
    * */
    @PostMapping("/newlink/{id}")
    public String newLink(@PathVariable("id") long id, @Valid QueryResult queryResult,
                          BindingResult bindingResult, Model model) {
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();
        boolean userPathsProvided = symLinkProperties.checkUserPaths();
        System.out.println("create link with: " + queryResult);
//        mediaLinksService.createSymLink()
        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);

        return "links";
    }

    /*
     * Redirect as a protection measure in case user reloads page
     * */
    @GetMapping(value = {"/newlink", "/newlink/{id}"})
    public String newLinkGet(@PathVariable(value = "id", required = false) Long id) {
        return "redirect:/links";
    }

    /*
     * Show all existing symlinks.
     *
     * */
    @GetMapping("/links")
    public String links(Model model) {
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();
        boolean userPathsProvided = symLinkProperties.checkUserPaths();

        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
        return "links";
    }

    @GetMapping("/config")
    public String configuration(Model model) {
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        List<MediaLink> allMediaLinks = mediaLinksService.getMediaLinks();
        boolean userPathsProvided = symLinkProperties.checkUserPaths();
        boolean userLinksPath = symLinkProperties.isUserLinksPath();
        boolean userTargetPath = symLinkProperties.isUserTargetPath();
        Path linksFolder = symLinkProperties.getLinksFolder();
        List<Path> targetFolderList = symLinkProperties.getTargetFolderList();

        model.addAttribute("query_list", allMediaQueries);
        model.addAttribute("link_list", allMediaLinks);
        model.addAttribute("user_paths", userPathsProvided);
        model.addAttribute("chk_user_target", userTargetPath);
        model.addAttribute("chk_user_links", userLinksPath);
        model.addAttribute("links_folder", linksFolder);
        model.addAttribute("target_folder_list", targetFolderList);
        return "config";
    }

    @PostMapping("/deletepath")
    public String deletePath(@RequestParam String path, Model model) {
        symLinkProperties.removeTargetPath(Path.of(path));
        System.out.println("delete " + path);
        return "redirect:/config";
    }

    //TODO report of falls back on default paths
    @PostMapping("/addpath")
    public String addPath(@RequestParam String path, Model model) {
        symLinkProperties.setTargetPath(Path.of(path));
        System.out.println("add " + path);
        return "redirect:/config";
    }

    @GetMapping("/tracker")
    public String getTrackerStatus(Model model) {
        trackerExecutor.startTracker();
//        model.addAttribute();
        return "tracker";
    }

}

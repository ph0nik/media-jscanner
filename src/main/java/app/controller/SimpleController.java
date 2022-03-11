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
import service.MediaLinksService;

import javax.validation.Valid;
import java.util.List;

@Controller
public class SimpleController {

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private MediaTrackerDao mediaTrackerDao;

    @Autowired
    private MediaLinksService mediaLinksService;

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("appName", appName);
        return "home";
    }

    /*
     * Show elements awaiting in the queue and let user select
     * file to process
     * */
    @GetMapping("/query")
    public String queryList(Model model) {
        List<MediaQuery> allMediaQueries = mediaTrackerDao.getAllMediaQueries();
        model.addAttribute("queryList", mediaTrackerDao.getAllMediaQueries());
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
        // get query by id from db
        MediaQuery queryById = mediaTrackerDao.getQueryById(id);
        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery(custom, queryById);
        model.addAttribute("resultslist", queryResults);
        model.addAttribute("selection", new QueryResult());
        model.addAttribute("query", queryById);
        return "result_selection";
    }

    /*
    * Create new link with query id and query result object.
    * */
    @PostMapping("/newlink/{id}")
    public String newLink(@PathVariable("id") long id, @Valid QueryResult queryResult,
                          BindingResult bindingResult, Model model) {
        System.out.println("create link with: " + queryResult);
//        mediaLinksService.createSymLink()
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        model.addAttribute("links", allMediaLinks);
        return "links";
    }

    /*
     * Show all existing symlinks.
     *
     * */
    @GetMapping("/links")
    public String links(Model model) {
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        model.addAttribute("links", allMediaLinks);
        return "links";
    }

    /*
     * Perform online search with provided custom query and present user
     * with results.
     * Prompt user to select title which will be used to create symlink.
     * */



}

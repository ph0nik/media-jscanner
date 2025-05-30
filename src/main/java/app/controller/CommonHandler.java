package app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import service.ErrorNotificationService;
import service.LiveDataService;
import service.MediaLinksService;
import service.PropertiesService;
import service.query.MovieQueryService;
import service.query.TvQueryService;

@ControllerAdvice
public class CommonHandler {
    @Autowired
    private MovieQueryService movieQueryService;
    @Autowired
    private TvQueryService tvQueryService;
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private LiveDataService liveDataService;
    @Autowired
    private ErrorNotificationService errorNotificationService;
    public static final String MOVIE = "/movie/";
    public static final String TV = "/tv/";
    public static final String LINKS = "/links/";
    public static final String IGNORED = "/ignored/";
    public static final String WIZARD = "/wizard";
    public static final String CONFIG = "/config";
    public static final String SCAN_FOR_MEDIA = "/scan-for-media";
    public static final String GLOBAL_SEARCH = "/global-search/";
    @ModelAttribute
    private void setMenuLinks(Model model) {
        model.addAttribute("tab_wizard", WIZARD);
        model.addAttribute("tab_movie", MOVIE);
        model.addAttribute("tab_tv", TV);
        model.addAttribute("tab_links", LINKS);
        model.addAttribute("tab_ignored", IGNORED);
        model.addAttribute("tab_config", CONFIG);
        model.addAttribute("movie_scan", SCAN_FOR_MEDIA);
        model.addAttribute("global_search", GLOBAL_SEARCH);
    }
    @ModelAttribute
    private void getCommonData(Model model) {
        model.addAttribute(
                "query_list_size",
                movieQueryService.getCurrentMediaQueries().size()
                        + tvQueryService.getCurrentMediaQueries().size()
        );
        model.addAttribute("query_list_movie", movieQueryService.getCurrentMediaQueries());
        model.addAttribute("query_list_tv", tvQueryService.getParentFolders()); // temp
        model.addAttribute("link_list", mediaLinksService.getMediaLinks());
        model.addAttribute("media_ignored", mediaLinksService.getMediaIgnoredList());
        model.addAttribute("user_paths", propertiesService.isMoviePathsProvided());
        model.addAttribute("error", errorNotificationService.getCurrentResult());
        model.addAttribute("future", liveDataService.getAutoMatcherFutureTask() == null
                || liveDataService.getAutoMatcherFutureTask().isDone());
        model.addAttribute("links_status", mediaLinksService.getStatusDto());
        model.addAttribute("error_links", mediaLinksService.getErrorLinks());
        // TODO set color to status - blue / orange regular - green added - red removed
    }




}

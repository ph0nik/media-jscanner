package app.controller;

import model.MediaLink;
import model.MediaQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import service.MediaLinksService;
import service.PropertiesService;
import service.query.MovieQueryService;
import service.query.TvQueryService;

import java.util.List;

@ControllerAdvice
public class CommonHandler {
    @Autowired
    private Router router;
    @Autowired
    private MovieQueryService movieQueryService;
    @Autowired
    private TvQueryService tvQueryService;
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;
    @Value("${tab.movies}")
    private String movie;
    @Value("${tab.tv}")
    private String tv;
    @Value("${tab.links}")
    private String links;
    @Value("${tab.ignored}")
    private String ignored;
    @Value("${tab.config}")
    private String config;
    @Value("${go.movie.scan}")
    private String scan;
    @ModelAttribute
    private void setMenuLinks(Model model) {
        model.addAttribute("tab_movie", movie);
        model.addAttribute("tab_tv", tv);
        model.addAttribute("tab_links", links);
        model.addAttribute("tab_ignored", ignored);
        model.addAttribute("tab_config", config);
        model.addAttribute("movie_scan", scan);
    }

    @ModelAttribute("router")
    public Router getRouter() {
        return router;
    }
    @ModelAttribute("query_list_size")
    public int getAllQueriesSize() {
        return movieQueryService.getCurrentMediaQueries().size() + tvQueryService.getCurrentMediaQueries().size();
    }
    @ModelAttribute("query_list_movie")
    public List<MediaQuery> getAllMediaQueries() {
        return movieQueryService.getCurrentMediaQueries();
    }
    @ModelAttribute("query_list_tv")
    public List<MediaQuery> getTvQueries() {
        return tvQueryService.getParentFolders();
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
    @ModelAttribute("api_token")
    public boolean isApiTokenPresent() {
        return propertiesService.checkApiToken();
    }


}

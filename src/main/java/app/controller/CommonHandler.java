package app.controller;

import model.MediaLink;
import model.MediaQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import service.ErrorNotificationService;
import service.MediaLinksService;
import service.PropertiesService;
import service.query.MovieQueryService;
import service.query.TvQueryService;

import java.util.List;

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
    private ErrorNotificationService errorNotificationService;
    public static final String MOVIE = "/movie";
    public static final String TV = "/tv";
    public static final String LINKS = "/links";
    public static final String IGNORED = "/ignored";
    public static final String CONFIG = "/config";
    public static final String SCAN_FOR_MEDIA = "/scan-for-media";
    @ModelAttribute
    private void setMenuLinks(Model model) {
        model.addAttribute("tab_movie", MOVIE);
        model.addAttribute("tab_tv", TV);
        model.addAttribute("tab_links", LINKS);
        model.addAttribute("tab_ignored", IGNORED);
        model.addAttribute("tab_config", CONFIG);
        model.addAttribute("movie_scan", SCAN_FOR_MEDIA);
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
    @ModelAttribute("user_paths") // TODO if user paths are not set redirect to config
    public boolean checkForUserProvidedPaths() {
        return propertiesService.userMoviePathsExist();
    }
    @ModelAttribute("error")
    public String getCurrentResult() {
        return errorNotificationService.getCurrentResult();
    }

// TODO implement handler interceptor to check folders and redirect user to config


}

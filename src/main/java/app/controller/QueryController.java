package app.controller;

import model.LastRequest;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.multipart.MultipartDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import service.AutoMatcherService;
import service.MediaConnectionService;
import service.MediaLinksService;
import service.PropertiesService;
import service.exceptions.NetworkException;
import service.query.MovieQueryService;
import service.query.TvQueryService;
import util.MediaIdentity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

@Controller

public class QueryController {
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private AutoMatcherService autoMatcherService;
    @Autowired
    private MovieQueryService movieQueryService;
    @Autowired
    private TvQueryService tvQueryService;
    @Autowired
    private MediaConnectionService mediaConnectionService;
    private static final String SEARCH_WITH_QUERY = "/search-query/";
    private static final String SELECT_QUERY = "/select-query/";
    private static final String SET_MULTI_PART = "/set-multipart";
    private static final String SKIP_MULTI_PART = "/skip-multipart";
    private static final String SEARCH_WITH_YEAR = "/search-with-year/";
    private static final String SEARCH_WITH_IMDB_LINK = "/imdb-link/";
    private static final String AUTO_MATCH = "/auto";
    private static final String NEW_MOVIE_LINK = "/new-link-movie/";
    private static final String PERSIST_NEW_MOVIE_LINKS = "/persist-new-links/";
    private static final String MARK_AS_IGNORED = "/new-ignore-movie/";
    private static final String AUTO_MATCH_FINISH = "/auto-match-finish/";
    @ModelAttribute
    private void setMenuLinks(Model model) {
        model.addAttribute("movie_search", SEARCH_WITH_QUERY);
        model.addAttribute("movie_select", SELECT_QUERY);
        model.addAttribute("movie_set_multi", SET_MULTI_PART);
        model.addAttribute("movie_skip_multi", SKIP_MULTI_PART);
        model.addAttribute("movie_search_year", SEARCH_WITH_YEAR);
        model.addAttribute("movie_imdb_id", SEARCH_WITH_IMDB_LINK);
        model.addAttribute("movie_auto", AUTO_MATCH);
        model.addAttribute("movie_link", NEW_MOVIE_LINK);
        model.addAttribute("movie_new_ignore", MARK_AS_IGNORED);
        model.addAttribute("movie_new_links", PERSIST_NEW_MOVIE_LINKS);
        model.addAttribute("movie_auto_finish", AUTO_MATCH_FINISH);
    }
    private Future<List<MediaLink>> future;
    private int sessionPageSize = 25;

    @GetMapping("/")
    public String redirectToQuery() {
        return "redirect:" + CommonHandler.MOVIE;
    }

    /*
     * Show elements awaiting in the queue and let user select
     * file to process or let auto matcher guess correct movies.
     * */
    @GetMapping(value = CommonHandler.MOVIE)
    public String movieQueryList(@RequestParam("page") Optional<Integer> page,
                                 @RequestParam("size") Optional<Integer> size,
                                 Model model) {

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(sessionPageSize);
        sessionPageSize = pageSize;
        int min = currentPage * pageSize - pageSize + 1;
        int max = currentPage * pageSize;
        Page<MediaQuery> paginatedMovieQueries = movieQueryService.getPageableQueries(
                PageRequest.of(currentPage - 1, pageSize), movieQueryService.getCurrentMediaQueries()
        );
//        Get Auto Matcher status
        // 1 * 20 - max, min - 1 * 20 - 20 + 1
        // 2 * 20 - max, min - 2 * 20 - 20 + 1
        // 1 – 25 of 106
        // (currentPage + 1) * pageSize - pageSize + 1 "-" (currentPage + 1) * pageSize of queryList.size
        boolean autoMatcherStatus = future == null || future.isDone();
        model.addAttribute("page", paginatedMovieQueries);
        model.addAttribute("future", autoMatcherStatus);
        model.addAttribute("page_min", min);
        model.addAttribute("page_max", max);
        model.addAttribute("current_page", page);
        return "query_list";
    }

    @PostMapping(value = SEARCH_WITH_QUERY)
    public String searchInQueryResults(@RequestParam("search") String search,
                                       Model model) {
        int min = 1;
        int max = sessionPageSize;
        // TODO move media query list to service and single object instance
        // after moving all live data to single object unify pagable
//        Page<MediaQuery> paginatedQueries = movieQueryService.findPaginatedQueries(
//                PageRequest.of(0, sessionPageSize),
//                movieQueryService.searchQuery(search));
        Page<MediaQuery> paginatedQueries = movieQueryService.getPageableQueries(
                PageRequest.of(0, sessionPageSize),
                movieQueryService.searchQuery(search));

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
    @PostMapping(value = SELECT_QUERY)
    public String selectQuery(@RequestParam String custom,
                              @RequestParam UUID uuid, Model model) throws NetworkException {
//        movieQueryService.setReferenceQuery(uuid);
//        model.addAttribute("query", movieQueryService.getReferenceQuery());
//        List<MediaQuery> groupedQueries = movieQueryService.getGroupedQueries(uuid);
//        if (groupedQueries.size() > 1) {
//            MultipartDto multipartDto = new MultipartDto();
//            multipartDto.setQueryUuid(uuid);
//            for (MediaQuery query : groupedQueries) {
//                multipartDto.addMultiPartElement(new MultiPartElement(query.getFilePath()));
//            }
//            model.addAttribute("multipart_dto", multipartDto);
//            return "query_multipart";
//        }
//        movieQueryService.addQueryToProcess(movieQueryService.getReferenceQuery());
//        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery(custom,
//                MediaIdentity.IMDB, movieQueryService);
//        model.addAttribute("result_list", queryResults);
//        model.addAttribute("query_result", new QueryResult());
//        return "result_selection";

        MultipartDto multiPartDto = mediaConnectionService.getMultiPartDto(uuid, movieQueryService);
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        if (multiPartDto != null) {
            model.addAttribute("multipart_dto", multiPartDto);
            return "query_multipart";
        }
        List<QueryResult> queryResults = mediaConnectionService.getResults(movieQueryService);
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";

    }

    @PostMapping(value = SET_MULTI_PART)
    public String setMultiPart(@ModelAttribute MultipartDto multipartDto, Model model) throws NetworkException {
//        movieQueryService.addQueriesToProcess(multipartDto.getMultiPartElementList());
//        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery("",
//                MediaIdentity.IMDB, movieQueryService);
        List<QueryResult> queryResults = mediaConnectionService.getMovieResults(multipartDto, movieQueryService);
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";
    }

    @GetMapping(value = SKIP_MULTI_PART)
    public String skipMultiPart(Model model) throws NetworkException {
//        movieQueryService.addQueryToProcess(movieQueryService.getReferenceQuery());
//        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery("",
//                MediaIdentity.IMDB, movieQueryService);
        List<QueryResult> queryResults = mediaConnectionService.getMovieResults(movieQueryService);
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";
    }

    @PostMapping(value = SEARCH_WITH_YEAR)
    public String searchTmdbWithYear(@RequestParam String custom,
                                     @RequestParam Optional<Integer> year,
                                     Model model) throws NetworkException {
        List<QueryResult> queryResults = mediaLinksService.searchTmdbWithTitleAndYear(custom,
                MediaIdentity.IMDB,
                year.orElse(0),
                movieQueryService);
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";
    }

    @PostMapping(value = SEARCH_WITH_IMDB_LINK)
    public String passImdbLink(@RequestParam String imdbLink, Model model) throws NetworkException {
        List<QueryResult> queryResults = mediaLinksService.searchWithImdbId(imdbLink,
                MediaIdentity.IMDB,
                movieQueryService);
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";
    }

    /*
     * GET mapping as a protection measure in case user reloads page
     * */
    @GetMapping(value = {SELECT_QUERY, SEARCH_WITH_YEAR})
    public String selectQueryGet(@PathVariable(value = "id", required = false) Long id, Model model) {
        LastRequest latestMediaQuery = mediaLinksService.getLatestMediaQueryRequest();
        if (latestMediaQuery == null) return "redirect:/";
//        MediaQuery lastMediaQuery = latestMediaQuery.getLastMediaQuery();
        model.addAttribute("result_list", latestMediaQuery.getLastRequest());
        model.addAttribute("query_result", new QueryResult());
        model.addAttribute("query", latestMediaQuery.getLastMediaQuery());
        return "result_selection";
    }

    @PostMapping(value = NEW_MOVIE_LINK)
    public String createLinkPath(QueryResult queryResult,
                                 BindingResult bindingResult,
                                 Model model) throws NetworkException {
        MediaIdentity mediaIdentity = (queryResult.getImdbId().isEmpty()) ? MediaIdentity.TMDB : MediaIdentity.IMDB;
        List<MediaLink> fileLink = mediaLinksService.createFileLink(queryResult,
                mediaIdentity,
                movieQueryService);
        movieQueryService.setMediaLinksToProcess(fileLink);
        model.addAttribute("file_link_to_process", fileLink);
        return "link_creation_confirm";
    }

    @GetMapping(value = PERSIST_NEW_MOVIE_LINKS)
    public String persistWithGivenListOfLinks(Model model) {
        mediaLinksService.persistsCollectedMediaLinks(movieQueryService);
        movieQueryService.clearMediaLinksToProcess();
        return "redirect:" + CommonHandler.MOVIE;
    }

    @GetMapping(value = NEW_MOVIE_LINK)
    public String newLinkGet(){
        return "redirect:" + CommonHandler.LINKS;
    }

    @PostMapping(value = MARK_AS_IGNORED)
    public String addToIgnoreList(@RequestParam UUID uuid, Model model)  {
        movieQueryService.setReferenceQuery(uuid);
        movieQueryService.addQueryToProcess(movieQueryService.getReferenceQuery());
        mediaLinksService.ignoreMediaFile(movieQueryService);
        return "redirect:/";
    }

    @GetMapping(value = CommonHandler.SCAN_FOR_MEDIA)
    public String scanFolders() {
        movieQueryService.scanForNewMediaQueries();
        tvQueryService.scanForNewMediaQueries();
        return "redirect:/";
    }

    @GetMapping(value = AUTO_MATCH)
    public String autoMatch(Model model) throws NetworkException {
        future = autoMatcherService.autoMatchAndGetLinks();
        boolean autoMatcherStatus = future == null || future.isDone();
        model.addAttribute("future", autoMatcherStatus);
        return "auto_matcher";
    }

    @GetMapping(value = AUTO_MATCH_FINISH)
    public String autoMatchShowLinks(Model model) {
        List<MediaLink> fileLink = movieQueryService.getMediaLinksToProcess();
        model.addAttribute("file_link_to_process", fileLink);
        return "link_creation_confirm";
    }
}

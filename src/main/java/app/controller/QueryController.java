package app.controller;

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
import service.exceptions.MissingReferenceMediaQueryException;
import service.exceptions.NetworkException;
import service.query.MovieQueryService;
import service.query.TvQueryService;
import util.MediaIdentifier;
import util.MediaType;

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
    private MediaConnectionService movieConnectionService;
    private static final String SEARCH_WITH_QUERY = "/search-query/";
    private static final String SELECT_QUERY = "/select-query/";
    private static final String SET_MULTI_PART = "/set-multipart";
    private static final String SKIP_MULTI_PART = "/skip-multipart";
    private static final String GENERAL_SEARCH = "/general-search/";
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
        model.addAttribute("movie_general_search", GENERAL_SEARCH);
        model.addAttribute("movie_search_year", SEARCH_WITH_YEAR);
        model.addAttribute("movie_imdb_id", SEARCH_WITH_IMDB_LINK);
        model.addAttribute("movie_auto", AUTO_MATCH);
        model.addAttribute("movie_link", NEW_MOVIE_LINK);
        model.addAttribute("movie_new_ignore", MARK_AS_IGNORED);
        model.addAttribute("movie_new_links", PERSIST_NEW_MOVIE_LINKS);
        model.addAttribute("movie_auto_finish", AUTO_MATCH_FINISH);
        model.addAttribute("current_menu", 0);
    }
    private Future<List<MediaLink>> future;
    private int sessionPageSize = 25;

    @GetMapping("/")
    public String redirectToQuery() {
        return "redirect:" + CommonHandler.MOVIE;
    }

    // TODO Allow user to add positions to basket for further grouping
    // Add "Group files" option, and then let user select files to group
    // group would be stored as basket until released from cache on regular basis
    // or explicitly removed by user - one by one or by clearing basket
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
//        int max = currentPage * pageSize;
        int max = Math.min(currentPage * pageSize, movieQueryService.getCurrentMediaQueries().size());
        Page<MediaQuery> paginatedMovieQueries = movieQueryService.getPageableQueries(
                PageRequest.of(currentPage - 1, pageSize),
                movieQueryService.getCurrentMediaQueries()
        );

//        Get Auto Matcher status
        // 1 * 20 - max, min - 1 * 20 - 20 + 1
        // 2 * 20 - max, min - 2 * 20 - 20 + 1
        // 1 – 25 of 106
        // (currentPage + 1) * pageSize - pageSize + 1 "-" (currentPage + 1) * pageSize of queryList.size
        model.addAttribute("page", paginatedMovieQueries);
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
        Page<MediaQuery> paginatedQueries = movieQueryService.getPageableQueries(
                PageRequest.of(0, sessionPageSize),
                movieQueryService.searchQuery(search)
        );
        model.addAttribute("page", paginatedQueries);
        model.addAttribute("page_min", min);
        model.addAttribute("page_max", max);
        return "query_list";
    }

    // TODO solve the case of existing link

    @PostMapping(value = CommonHandler.GLOBAL_SEARCH)
    public String searchAllDatabase(@RequestParam("search") String search, Model model) {
        List<MediaQuery> mediaQueriesSearchResult = movieQueryService.searchQuery(search);
        List<MediaLink> mediaLinksSearchResult = mediaLinksService.searchMediaLinks(search);
        List<MediaLink> mediaIgnoredSearchResult = mediaLinksService.searchMediaIgnoredList(search);
        model.addAttribute("query_search", search);
        model.addAttribute("query_list_search", mediaQueriesSearchResult);
        model.addAttribute("link_list_search", mediaLinksSearchResult);
        model.addAttribute("media_ignored_search", mediaIgnoredSearchResult);
        return "search_internal";
    }

    // TODO add edit of existing link

    /*
     * For selected file perform online search for matching titles.
     * Present returned results and prompt user to select which title
     * will be used to create symlink.
     * */
    @PostMapping(value = SELECT_QUERY)
    public String selectQuery(@RequestParam String custom,
                              @RequestParam UUID uuid, Model model)
            throws NetworkException, MissingReferenceMediaQueryException {
        movieQueryService.setReferenceQuery(uuid);
/*
 TODO select query > check multipart > set multipart > set reference queries > search web > result selection
      select query > check multipart > set reference queries > search web > result selection
          before search web save multipart state
          abort option > clear process and reference query
*/
        MultipartDto multiPartDto = movieConnectionService.multiPartDtoBuilder(
                uuid, movieQueryService, MediaType.MOVIE
        );
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        if (multiPartDto != null) {
            model.addAttribute("multipart_dto", multiPartDto);
            return "query_multipart";
        }

        List<QueryResult> queryResults = movieConnectionService.getResults(movieQueryService);
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";

    }

    @PostMapping(value = SET_MULTI_PART)
    public String setMultiPart(@ModelAttribute MultipartDto multipartDto, Model model)
            throws NetworkException, MissingReferenceMediaQueryException {
        List<QueryResult> queryResults = movieConnectionService.getMultipleFilesResults(
                multipartDto, movieQueryService
        );
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";
    }

    @GetMapping(value = SKIP_MULTI_PART)
    public String skipMultiPart(Model model) throws NetworkException, MissingReferenceMediaQueryException {
        List<QueryResult> queryResults = movieConnectionService.getResults(movieQueryService);
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";
    }

    @PostMapping(value = SEARCH_WITH_YEAR)
    public String searchTmdbWithYear(@RequestParam String custom,
                                     @RequestParam Optional<Integer> year,
                                     Model model) throws NetworkException, MissingReferenceMediaQueryException {
        List<QueryResult> resultsCustomSearchTmdb = movieConnectionService.getResultsCustomSearchTmdb(
                movieQueryService, custom, year
        );
        model.addAttribute("result_list", resultsCustomSearchTmdb);
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";
    }

    // TODO differentiate badges for query results depending on tv or movie
    @PostMapping(value = SEARCH_WITH_IMDB_LINK)
    public String passImdbLink(@RequestParam String imdbLink, Model model)
            throws NetworkException, MissingReferenceMediaQueryException {
        List<QueryResult> resultsImdbLinkSearch = movieConnectionService.getResultsImdbLinkSearch(
                imdbLink, movieQueryService
        );
        model.addAttribute("result_list", resultsImdbLinkSearch);
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        model.addAttribute("query_result", new QueryResult());
        return "result_selection";
    }

    /*
     * GET mapping as a protection measure in case user reloads page
     * */
    @GetMapping(value = {SELECT_QUERY, SEARCH_WITH_YEAR})
    public String selectQueryGet(@PathVariable(value = "id", required = false) Long id, Model model)
            throws MissingReferenceMediaQueryException {
        if (mediaLinksService.getLatestMediaQueryRequest().isEmpty()) return "redirect:/";
        model.addAttribute("result_list", mediaLinksService.getLatestMediaQueryRequest());
        model.addAttribute("query_result", new QueryResult());
        model.addAttribute("query", movieQueryService.getReferenceQuery());
        return "result_selection";
    }

    @PostMapping(value = NEW_MOVIE_LINK)
    public String createLinkPath(QueryResult queryResult,
                                 BindingResult bindingResult,
                                 Model model) throws NetworkException {
        MediaIdentifier mediaIdentifier = (queryResult.getImdbId().isEmpty()) ? MediaIdentifier.TMDB : MediaIdentifier.IMDB;
        List<MediaLink> fileLinks = mediaLinksService.createFileLink(queryResult,
                mediaIdentifier,
                movieQueryService);
        mediaLinksService.setMediaLinksToProcess(fileLinks);
        model.addAttribute("file_link_to_process", fileLinks);
        return "link_creation_confirm";
    }

    @GetMapping(value = PERSIST_NEW_MOVIE_LINKS)
    public String persistWithGivenListOfLinks(Model model) {
        mediaLinksService.persistsCollectedMediaLinks(movieQueryService);
        return "redirect:" + CommonHandler.MOVIE;
    }

    @GetMapping(value = NEW_MOVIE_LINK)
    public String newLinkGet(){
        return "redirect:" + CommonHandler.LINKS;
    }

    @PostMapping(value = MARK_AS_IGNORED)
    public String addToIgnoreList(@RequestParam UUID uuid, Model model)
            throws MissingReferenceMediaQueryException {
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

    @GetMapping(value = CommonHandler.WIZARD)
    public String openWizard(Model model) {
        model.addAttribute("current_menu", 4);
        boolean autoMatcherStatus = future == null || future.isDone();
        return "auto_matcher";
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
        List<MediaLink> fileLink = mediaLinksService.getMediaLinksToProcess();
        model.addAttribute("file_link_to_process", fileLink);
        return "link_creation_confirm";
    }
}

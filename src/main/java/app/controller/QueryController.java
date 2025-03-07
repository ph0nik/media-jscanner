package app.controller;

import model.LastRequest;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.multipart.MultipartDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import service.*;
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
    @Autowired
    private ErrorNotificationService errorNotificationService;
    @Value("${go.movie.search}")
    private String search;
    @Value("${go.movie.select}")
    private String select;
    @Value("${go.movie.setmulti}")
    private String setMulti;
    @Value("${go.movie.skipmulti}")
    private String skipMulti;
    @Value("${go.movie.withyear}")
    private String searchWithYear;
    @Value("${go.movie.imdb}")
    private String imdbSearch;
    @Value("${go.movie.auto}")
    private String auto;
    @Value("${go.movie.link}")
    private String movieNewLink;
    @Value("${go.movie.ignore}")
    private String movieNewIgnore;
    @ModelAttribute
    private void setMenuLinks(Model model) {
        model.addAttribute("movie_search", search);
        model.addAttribute("movie_select", select);
        model.addAttribute("movie_set_multi", setMulti);
        model.addAttribute("movie_skip_multi", skipMulti);
        model.addAttribute("movie_search_year", searchWithYear);
        model.addAttribute("movie_imdb_id", imdbSearch);
        model.addAttribute("movie_auto", auto);
        model.addAttribute("movie_link", movieNewLink);
        model.addAttribute("movie_new_ignore", movieNewIgnore);
    }
    @ModelAttribute("error")
    public String getCurrentResult() {
        return errorNotificationService.getCurrentResult();
    }

    private Future<List<MediaLink>> future;
    private int sessionPageSize = 25;

    @GetMapping("/")
    // TODO change variables in templates
    public String redirectToQuery() {
        return "redirect:/movie";
    }

    /*
     * Show elements awaiting in the queue and let user select
     * file to process or let auto matcher guess correct movies.
     * */
    @GetMapping("${tab.movies}")
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

    @PostMapping("${go.movie.search}")
    public String searchInQueryResults(@RequestParam("search") String search,
                                       Model model) {
        int min = 1;
        int max = sessionPageSize;
        // TODO move media query list to service and single object instance
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
    @PostMapping("${go.movie.select}")
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

    @PostMapping("${go.movie.setmulti}")
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

    @GetMapping("${go.movie.skipmulti}")
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

    @PostMapping("${go.movie.withyear}")
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

    @PostMapping("${go.movie.imdb}")
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
    @GetMapping(value = {"${go.movie.select}", "${go.movie.withyear}"})
    public String selectQueryGet(@PathVariable(value = "id", required = false) Long id, Model model) {
        LastRequest latestMediaQuery = mediaLinksService.getLatestMediaQueryRequest();
        if (latestMediaQuery == null) return "redirect:/";
        MediaQuery lastMediaQuery = latestMediaQuery.getLastMediaQuery();
        model.addAttribute("result_list", latestMediaQuery.getLastRequest());
        model.addAttribute("query_result", new QueryResult());
        model.addAttribute("query", lastMediaQuery);
        return "result_selection";
    }

    @PostMapping("${go.movie.link}")
    public String newLink(QueryResult queryResult,
                          BindingResult bindingResult,
                          Model model) throws NetworkException {
        MediaIdentity mediaIdentity = (queryResult.getImdbId().isEmpty()) ? MediaIdentity.TMDB : MediaIdentity.IMDB;
        int operations = mediaLinksService.createFileLink(queryResult,
                mediaIdentity,
                movieQueryService);
        // TODO implement list of results
//        operationResults.forEach(lcr -> errorNotificationService.setLinkCreationResult(lcr));
        return "redirect:/movie";
    }

    @GetMapping("${go.movie.link}")
    public String newLinkGet(){
        return "redirect:/links";
    }

    @PostMapping("${go.movie.ignore}")
    public String addToIgnoreList(@RequestParam UUID uuid, Model model)  {
        movieQueryService.setReferenceQuery(uuid);
        movieQueryService.addQueryToProcess(movieQueryService.getReferenceQuery());
        mediaLinksService.ignoreMediaFile(movieQueryService);
        return "redirect:/";
    }

    @GetMapping("${go.movie.scan}")
    public String scanFolders() {
        movieQueryService.scanForNewMediaQueries();
        tvQueryService.scanForNewMediaQueries();
        return "redirect:/";
    }

    @GetMapping("${go.movie.auto}")
    public String autoMatch() throws NetworkException {
        // TODO secure in case of network problems, funky stuff with the files
        future = autoMatcherService.autoMatchFilesWithFuture();
        return "redirect:/";
    }
}

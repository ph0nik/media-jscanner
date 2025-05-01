package app.controller;

import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.links.MediaLinkDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.*;
import service.exceptions.NetworkException;
import service.exceptions.NoQueryFoundException;
import service.query.TvQueryService;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

@Controller
@SessionAttributes("query_result")
public class QueryTvController {
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;
    @Autowired
    private AutoMatcherService autoMatcherService;
    @Autowired
    private MediaConnectionService mediaConnectionService;
    @Autowired
    private TvQueryService tvQueryService;
    private static final String SELECT_QUERY = "/select-tv-query/";
    private static final String SEARCH_WITH_QUERY = "/search-query-tv/";
    private static final String SEARCH_WITH_CUSTOM = "/search-query-tv-custom/";
    private static final String SELECT_EPISODES = "/select-tv-episodes/";
    private static final String SET_MULTI_PART = "/set-tv-multipart";
    private static final String SKIP_MULTI_PART = "/skip-tv-multipart";
    private static final String SEARCH_WITH_YEAR = "/search-tv-with-year/";
    private static final String SEARCH_WITH_IMDB_LINK = "/tv-imdb-link/";
    private static final String NEW_TV_LINK = "/new-link-tv/";
    private static final String NEW_TV_IGNORE = "/new-ignore-tv/";
    @ModelAttribute
    private void setMenuLinks(Model model) {
        model.addAttribute("tv_search", SEARCH_WITH_QUERY);
        model.addAttribute("tv_select", SELECT_QUERY);
        model.addAttribute("tv_custom_search", SEARCH_WITH_CUSTOM);
        model.addAttribute("tv_match_ep", SELECT_EPISODES);
        model.addAttribute("tv_set_multi", SET_MULTI_PART);
        model.addAttribute("tv_skip_multi", SKIP_MULTI_PART);
        model.addAttribute("tv_search_year", SEARCH_WITH_YEAR);
        model.addAttribute("tv_imdb", SEARCH_WITH_IMDB_LINK);
        model.addAttribute("tv_new_ignore", NEW_TV_IGNORE);
        model.addAttribute("current_menu", 1);
    }
    private Future<List<MediaLink>> future;
    private int sessionPageSize = 25;
    private Page<MediaQuery> paginatedTvQueries;

    // TODO after selecting title for file, check if this id exist and
    // ask weather to change existing link to a new one or create new link

    @GetMapping(value = CommonHandler.TV)
    public String queryList(@RequestParam("page") Optional<Integer> page,
                            @RequestParam("size") Optional<Integer> size,
                            Model model) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(sessionPageSize);
        sessionPageSize = pageSize;
        int min = currentPage * pageSize - pageSize + 1;
        int max = currentPage * pageSize;
        paginatedTvQueries = tvQueryService.getPageableQueries(
                PageRequest.of(currentPage - 1, pageSize),
                tvQueryService.getParentFolders()
        );
//        Get Auto Matcher status
        // 1 * 20 - max, min - 1 * 20 - 20 + 1
        // 2 * 20 - max, min - 2 * 20 - 20 + 1
        // 1 – 25 of 106
        // (currentPage + 1) * pageSize - pageSize + 1 "-" (currentPage + 1) * pageSize of queryList.size
        boolean autoMatcherStatus = future == null || future.isDone();
        model.addAttribute("page", paginatedTvQueries);
        model.addAttribute("future", autoMatcherStatus);
        model.addAttribute("page_min", min);
        model.addAttribute("page_max", max);
        return "query_tv_list";
    }

    @PostMapping(value = SEARCH_WITH_QUERY)
    public String searchQuery(@RequestParam("search") String search,
                              Model model) {
        paginatedTvQueries = tvQueryService.getPageableQueries(
                PageRequest.of(0, sessionPageSize),
                tvQueryService.searchQuery(search));
        boolean autoMatcherStatus = future == null || future.isDone();
        model.addAttribute("page", paginatedTvQueries);
        model.addAttribute("future", autoMatcherStatus);
        model.addAttribute("page_min", 1);
        model.addAttribute("page_max", sessionPageSize);
        return "query_tv_list";
    }

    @PostMapping(value = SELECT_QUERY)
    public String selectQuery(@RequestParam String path, Model model) throws NoQueryFoundException, NetworkException {
        tvQueryService.setUpQueryReference(path);
        List<QueryResult> queryResults = mediaConnectionService.getResults(tvQueryService);
        model.addAttribute("season", tvQueryService.getSeasonTv());
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query", tvQueryService.getReferenceQuery());
        model.addAttribute("query_result", new QueryResult());
        return "result_selection_tv";
    }

    @PostMapping(value = SEARCH_WITH_CUSTOM)
    public String customTvSearchWeb(@RequestParam String custom,
                                    Model model) throws NetworkException {
        System.out.println("try custom: " + custom);
        List<QueryResult> queryResults = mediaConnectionService.getResultsCustomSearchWeb(tvQueryService, custom);
        model.addAttribute("season", tvQueryService.getSeasonTv());
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query", tvQueryService.getReferenceQuery());
        model.addAttribute("query_result", new QueryResult());
        return "result_selection_tv";
    }

    @PostMapping(value = SEARCH_WITH_YEAR)
    public String customTvSearchWithYear(@RequestParam String custom,
                                         @RequestParam Optional<Integer> year,
                                         Model model) throws NetworkException {
        List<QueryResult> queryResults = mediaConnectionService.getResultsCustomSearchTmdb(tvQueryService,
                custom, year);
        model.addAttribute("season", tvQueryService.getSeasonTv());
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query", tvQueryService.getReferenceQuery());
        model.addAttribute("query_result", new QueryResult());
        return "result_selection_tv";
    }

    @PostMapping(value = SELECT_EPISODES)
    public String selectEpisodes(@ModelAttribute("query_result") QueryResult queryResult, Model model)
            throws FileNotFoundException, NetworkException {
        int seasonNumber = queryResult.getMultipart();
        queryResult = mediaLinksService.getTvDetails(queryResult, seasonNumber);
        List<MediaLink> mediaLinksTv = mediaLinksService.createMediaLinksTv(queryResult, seasonNumber, tvQueryService);
//        System.out.println("before");
//        mediaLinksTv.forEach(System.out::println);
        MediaLinkDto mediaLinksDto = mediaConnectionService.getMediaLinksDto(mediaLinksTv);
        model.addAttribute("media_links_dto", mediaLinksDto);
        model.addAttribute("query", tvQueryService.getReferenceQuery());
        model.addAttribute("query_result", queryResult);
        // TODO if no mapping on episodes found just count elements, after changes show user the same view with new values
        // create state service for currently evaluated media links
        return "episode_selection";
    }

    @PostMapping(value = SET_MULTI_PART)
    public String checkEpisodes(@ModelAttribute MediaLinkDto mediaLinksDto,
                                @ModelAttribute("query_result") QueryResult queryResult,
                                Model model) {
        System.out.println("after setting multi: " + queryResult);
        mediaLinksDto.getMediaLinkDtos().forEach(x -> System.out.println(x.getMediaLink()));
        // TODO creates query result with contructor with parameter
//        mediaLinksService.getLatestMediaQueryRequest().getLastRequest().stream().forEach(System.out::println);


//        mediaConnectionService.checkEpisodesOrdering(mediaLinksDto);
//        if (false) -> return
//                if true process forward
//        tvQueryService.addQueriesToProcess(mediaLinksDto.getMultiPartElementList());
//        System.out.println(tvQueryService.getProcessList());
//        System.out.println(tvQueryService.getReferenceQuery());
//        tvQueryService.addQueriesToProcess(multipartDto.getMultiPartElementList());
//        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery("",
//                MediaIdentity.IMDB, movieQueryService);
//        model.addAttribute("query", movieQueryService.getReferenceQuery());
//        model.addAttribute("result_list", queryResults);
//        model.addAttribute("query_result", new QueryResult());
        return "redirect:/tv";
    }

    @PostMapping(value = NEW_TV_IGNORE)
    public String addToIgnoreList(@RequestParam UUID uuid, Model model)  {
        System.out.println("ignore tv" + uuid);
        tvQueryService.setReferenceQuery(uuid);
        System.out.println(tvQueryService.getReferenceQuery());
        tvQueryService.addQueryToProcess(tvQueryService.getReferenceQuery());
        mediaLinksService.ignoreMediaFile(tvQueryService);
        return "redirect:/";
    }
}

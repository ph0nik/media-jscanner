package app.controller;

import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.links.MediaLinkDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Autowired
    private ErrorNotificationService errorNotificationService;
    @Value("${go.tv.search}")
    private String tvQuerySearch;
    @Value("${go.tv.select}")
    private String tvQuerySelect;
    @Value("${go.tv.search-custom}")
    private String tvQueryCustom;
    @Value("${go.tv.episodes}")
    private String tvMatchEpisodes;
    @Value("${go.tv.setmulti}")
    private String tvSetMulti;
    @Value("${go.tv.skipmulti}")
    private String tvSkipMulti;
    @Value("${go.tv.withyear}")
    private String tvWithYear;
    @Value("${go.tv.imdb}")
    private String tvImdbSearch;
    @Value("${go.tv.ignore}")
    private String tvNewIgnore;
    @ModelAttribute
    private void setMenuLinks(Model model) {
        model.addAttribute("tv_search", tvQuerySearch);
        model.addAttribute("tv_select", tvQuerySelect);
        model.addAttribute("tv_custom_search", tvQueryCustom);
        model.addAttribute("tv_match_ep", tvMatchEpisodes);
        model.addAttribute("tv_set_multi", tvSetMulti);
        model.addAttribute("tv_skip_multi", tvSkipMulti);
        model.addAttribute("tv_search_year", tvWithYear);
        model.addAttribute("tv_imdb", tvImdbSearch);
        model.addAttribute("tv_new_ignore", tvNewIgnore);
    }
    private Future<List<MediaLink>> future;
    private int sessionPageSize = 25;
    private Page<MediaQuery> paginatedTvQueries;

    // TODO after selecting title for file, check if this id exist and
    // ask weather to change existing link to a new one or create new link

    @GetMapping("${tab.tv}")
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

    @PostMapping("${go.tv.search}")
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

    @PostMapping("${go.tv.select}")
    public String selectQuery(@RequestParam String path, Model model) throws NoQueryFoundException, NetworkException {
        tvQueryService.setUpQueryReference(path);
        List<QueryResult> queryResults = mediaConnectionService.getResults(tvQueryService);
        model.addAttribute("season", tvQueryService.getSeasonTv());
        model.addAttribute("result_list", queryResults);
        model.addAttribute("query", tvQueryService.getReferenceQuery());
        model.addAttribute("query_result", new QueryResult());
        return "result_selection_tv";
    }

    @PostMapping("${go.tv.search-custom}")
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

    @PostMapping("${go.tv.withyear}")
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

    @PostMapping("${go.tv.episodes}")
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

    @PostMapping("${go.tv.setmulti}")
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

    @PostMapping("${go.tv.ignore}")
    public String addToIgnoreList(@RequestParam UUID uuid, Model model)  {
        System.out.println("ignore tv" + uuid);
        tvQueryService.setReferenceQuery(uuid);
        System.out.println(tvQueryService.getReferenceQuery());
        tvQueryService.addQueryToProcess(tvQueryService.getReferenceQuery());
        mediaLinksService.ignoreMediaFile(tvQueryService);
        return "redirect:/";
    }
}

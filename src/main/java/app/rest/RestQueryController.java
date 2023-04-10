package app.rest;

import model.MediaQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import service.MediaLinksService;
import service.MediaQueryService;
import service.PropertiesService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/rest")
public class RestQueryController {

    private static final int DEFAULT_PAGE_SIZE = 50;
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private MediaQueryService mediaQueryService;

    @GetMapping(value = "/page={page}", produces = "application/json")
    public Page<MediaQuery> queries(@PathVariable("page") int page) {
        Page<MediaQuery> paginatedQueries = mediaLinksService.findPaginatedQueries(PageRequest.of(page - 1, DEFAULT_PAGE_SIZE), mediaLinksService.getMediaQueryList());
        return paginatedQueries;
//        return paginatedQueries.getContent();
    }

    @GetMapping(value = "/search={query}", produces = "application/json")
    public List<MediaQuery> searchQuery(@PathVariable("query") String query) {
        List<MediaQuery> mediaQueries = mediaQueryService.searchQuery(query);
        Page<MediaQuery> paginatedQueries = mediaLinksService.findPaginatedQueries(PageRequest.of(0, DEFAULT_PAGE_SIZE), mediaQueries);
        return paginatedQueries.get().collect(Collectors.toList());
    }

    @GetMapping(value = "/select={query_id}", produces = "application/json")
    public List<MediaQuery> selectQuery(@PathVariable("query_id") UUID queryId) {
        mediaQueryService.setReferenceQuery(queryId);
        List<MediaQuery> groupedQueries = mediaQueryService.getGroupedQueries(queryId);
        // TODO set multipart
        System.out.println(groupedQueries);
        return groupedQueries;
    }

    @GetMapping(value = "/refresh", produces = "application/json")
    public ModelAndView refreshQueries() {
        mediaQueryService.scanForNewMediaQueries();
        return new ModelAndView("redirect:/rest/page=1");
    }
}

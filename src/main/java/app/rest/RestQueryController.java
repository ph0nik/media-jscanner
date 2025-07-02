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
import service.query.MovieQueryService;
import service.PropertiesService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/rest")
public class RestQueryController {

    private final int defaultPageSize = 50;
    @Autowired
    private MediaLinksService mediaLinksService;
    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private MovieQueryService movieQueryService;

//    @GetMapping(value = "/page={page}", produces = "application/json")
//    public Page<MediaQuery> queries(@PathVariable("page") int page) {
//        return movieQueryService.getPageableQueries(PageRequest.of(page - 1, defaultPageSize), mediaLinksService.getMediaQueryList());
////        return paginatedQueries.getContent();
//    }

    @GetMapping(value = "/search={query}", produces = "application/json")
    public List<MediaQuery> searchQuery(@PathVariable("query") String query) {
        List<MediaQuery> mediaQueries = movieQueryService.searchQuery(query);
        Page<MediaQuery> paginatedQueries = movieQueryService.getPageableQueries(PageRequest.of(0, defaultPageSize), mediaQueries);
        return paginatedQueries.get().collect(Collectors.toList());
    }

    @GetMapping(value = "/select={query_id}", produces = "application/json")
    public List<MediaQuery> selectQuery(@PathVariable("query_id") UUID queryId) {
        movieQueryService.setReferenceQuery(queryId);
        List<MediaQuery> groupedQueries = movieQueryService.getGroupedQueriesWithId(queryId);
        // TODO set multipart
        return groupedQueries;
    }

    @GetMapping(value = "/refresh", produces = "application/json")
    public ModelAndView refreshQueries() {
        movieQueryService.scanForNewMediaQueries();
        return new ModelAndView("redirect:/rest/page=1");
    }
}

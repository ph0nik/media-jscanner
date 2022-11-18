package service;

import model.MediaQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PaginationImpl implements Pagination {

//    @Autowired
//    @Qualifier("spring")
//    private MediaTrackerDao mediaTrackerDao;
//    @Autowired
//    private MediaQueryService mediaQueryService;
//    private List<MediaQuery> allMediaQueries;

//    PaginationImpl(MediaTrackerDao mediaTrackerDao, MediaQueryService mediaQueryService) {
//        this.mediaTrackerDao = mediaTrackerDao;
//        this.mediaQueryService = mediaQueryService;
//    }

    @Override
    public Page<MediaQuery> findPaginatedQueries(Pageable pageable, List<MediaQuery> mediaQueryList) {

//        List<MediaQuery> allMediaQueries = mediaTrackerDao.getAllMediaQueries();
//        List<MediaQuery> allMediaQueries = mediaQueryService.getCurrentMediaQueries();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<MediaQuery> list;

        if (mediaQueryList.size() < startItem) {
            list = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, mediaQueryList.size());
            list = mediaQueryList.subList(startItem, toIndex);
        }

        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), mediaQueryList.size());
    }
}

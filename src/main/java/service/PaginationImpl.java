package service;

import dao.MediaTrackerDao;
import model.MediaQuery;
import org.springframework.data.domain.*;

import java.util.List;

public class PaginationImpl implements Pagination {

    private final MediaTrackerDao mediaTrackerDao;

    PaginationImpl(MediaTrackerDao mediaTrackerDao) {
        this.mediaTrackerDao = mediaTrackerDao;
    }

    @Override
    public Page<MediaQuery> findPaginatedQueries(Pageable pageable) {
        List<MediaQuery> allMediaQueries = mediaTrackerDao.getAllMediaQueries();
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<MediaQuery> list;

        if (allMediaQueries.size() < startItem) {
            list = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, allMediaQueries.size());
            list = allMediaQueries.subList(startItem, toIndex);
        }

        Page<MediaQuery> mediaQueryPage =
                new PageImpl<>(list, PageRequest.of(currentPage, pageSize), allMediaQueries.size());

        return mediaQueryPage;
    }
}

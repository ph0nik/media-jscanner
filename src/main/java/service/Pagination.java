package service;

import model.MediaQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface Pagination {

    Page<MediaQuery> findPaginatedQueries(Pageable pageable);
}

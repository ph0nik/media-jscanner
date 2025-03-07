package service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface Pagination<T> {

    public Page<T> getPage(Pageable pageable, List<T> mediaQueryList);
//    Page<MediaQuery> findPaginatedQueries(Pageable pageable, List<MediaQuery> mediaQueryList);
//
//    Page<MediaLink> findPaginatedLinks(Pageable pageable, List<MediaLink> mediaLinks);
}

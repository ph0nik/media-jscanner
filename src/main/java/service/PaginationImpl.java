package service;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class PaginationImpl<T> implements Pagination<T> {

    public Page<T> getPage(Pageable pageable, List<T> mediaQueryList) {
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<T> list;
        if (mediaQueryList.size() < startItem) {
            list = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, mediaQueryList.size());
            list = mediaQueryList.subList(startItem, toIndex);
        }
        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), mediaQueryList.size());
    }

//    @Override
//    public Page<MediaQuery> findPaginatedQueries(Pageable pageable, List<MediaQuery> mediaQueryList) {
//        int pageSize = pageable.getPageSize();
//        int currentPage = pageable.getPageNumber();
//        int startItem = currentPage * pageSize;
//        List<MediaQuery> list;
//        if (mediaQueryList.size() < startItem) {
//            list = List.of();
//        } else {
//            int toIndex = Math.min(startItem + pageSize, mediaQueryList.size());
//            list = mediaQueryList.subList(startItem, toIndex);
//        }
//        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), mediaQueryList.size());
//    }
//
//    @Override
//    public Page<MediaLink> findPaginatedLinks(Pageable pageable, List<MediaLink> mediaLinks) {
//        int pageSize = pageable.getPageSize();
//        int currentPage = pageable.getPageNumber();
//        int startItem = currentPage * pageSize;
//        List<MediaLink> list;
//        if (mediaLinks.size() < startItem) {
//            list = List.of();
//        } else {
//            int toIndex = Math.min(startItem + pageSize, mediaLinks.size());
//            list = mediaLinks.subList(startItem, toIndex);
//        }
//        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), mediaLinks.size());
//    }


}

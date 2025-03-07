package service;

import model.MediaLink;
import model.QueryResult;
import model.links.MediaLinkDto;
import model.multipart.MultipartDto;
import service.exceptions.NetworkException;
import service.query.MediaQueryService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface MediaConnectionService {

    public MultipartDto getMultiPartDto(UUID uuid, MediaQueryService mediaQueryService);

    MediaLinkDto getMediaLinksDto(List<MediaLink> mediaLinks);

    List<QueryResult> getMovieResults(MultipartDto multipartDto, MediaQueryService mediaQueryService) throws NetworkException;

    List<QueryResult> getMovieResults(MediaQueryService mediaQueryService) throws NetworkException;

    List<QueryResult> getResultsCustomSearchTmdb(MediaQueryService mediaQueryService,
                                                 String custom, Optional<Integer> year) throws NetworkException;

    List<QueryResult> getResultsCustomSearchWeb(MediaQueryService mediaQueryService, String custom) throws NetworkException;

    public List<QueryResult> getResults(MediaQueryService mediaQueryService) throws NetworkException;
}

package service;

import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.links.MediaLinkDto;
import model.links.TvMediaLinkDto;
import model.multipart.MultipartDto;
import org.springframework.stereotype.Service;
import service.exceptions.NetworkException;
import service.query.MediaQueryService;
import util.MediaIdentifier;
import util.MediaType;
import util.TextExtractTools;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MovieConnectionService implements MediaConnectionService {

    private final MediaLinksService mediaLinksService;

    public MovieConnectionService(MediaLinksService mediaLinksService) {
        this.mediaLinksService = mediaLinksService;
    }

    @Override
    public MultipartDto multiPartDtoBuilder(
            UUID uuid,
            MediaQueryService mediaQueryService,
            MediaType mediaType
    ) {
//        mediaQueryService.setReferenceQuery(uuid);
        List<MediaQuery> groupedQueries = mediaQueryService.getGroupedQueriesWithId(uuid);
        if (groupedQueries.size() > 1) {
            MultipartDto multipartDto = new MultipartDto();
            multipartDto.setQueryUuid(uuid);
            int counter = 1;
            for (MediaQuery mediaQuery : groupedQueries) {
                multipartDto.addMultiPartElement(mediaQuery, counter++, mediaType);
            }
            return multipartDto;
        }
        return null;
    }

    @Override
    public MediaLinkDto getMediaLinksDto(List<MediaLink> mediaLinks) {
        List<TvMediaLinkDto> collect = mediaLinks.stream()
                .map(ml -> new TvMediaLinkDto(ml, TextExtractTools.extractEpisodeNumber(ml.getLinkPath())))
                .collect(Collectors.toList());
        MediaLinkDto mediaLinkDto = new MediaLinkDto();
        mediaLinkDto.setMediaLinkDtos(collect);
        return mediaLinkDto;
    }


    @Override
    public List<QueryResult> getMultipleFilesResults(MultipartDto multipartDto, MediaQueryService mediaQueryService) throws NetworkException {
        mediaQueryService.addQueriesToProcess(multipartDto.getMultiPartElementList());
        return mediaLinksService.executeMediaQuery("",
                MediaIdentifier.IMDB, mediaQueryService);
    }

//    @Override
//    public List<QueryResult> getMovieResults(MediaQueryService mediaQueryService) throws NetworkException {
//        mediaQueryService.addQueryToProcess(mediaQueryService.getReferenceQuery());
//        return mediaLinksService.executeMediaQuery("",
//                MediaIdentifier.IMDB, mediaQueryService);
//    }

    @Override
    public List<QueryResult> getResults(MediaQueryService mediaQueryService) throws NetworkException {
        mediaQueryService.addQueryToProcess(mediaQueryService.getReferenceQuery());
        return mediaLinksService.executeMediaQuery("",
                MediaIdentifier.IMDB, mediaQueryService);
    }

    @Override
    public List<QueryResult> getResultsCustomSearchTmdb(
            MediaQueryService mediaQueryService, String custom, Optional<Integer> year) throws NetworkException {
//        mediaQueryService.addQueryToProcess(mediaQueryService.getReferenceQuery());
        if (mediaQueryService.getReferenceQuery() != null) {
            return mediaLinksService.searchTmdbWithTitleAndYear(custom,
                    MediaIdentifier.IMDB,
                    year.orElse(0),
                    mediaQueryService);
        }
        return List.of();
    }

    @Override
    public List<QueryResult> getResultsImdbLinkSearch(
            String imdbLink, MediaQueryService mediaQueryService) throws NetworkException {
        // check if imdb link is valid
        return mediaLinksService.searchWithImdbId(
                imdbLink,
                MediaIdentifier.IMDB,
                mediaQueryService);
    }

    @Override
    public List<QueryResult> getResultsCustomSearchWeb(
            MediaQueryService mediaQueryService, String custom) throws NetworkException {
//        mediaQueryService.addQueryToProcess(mediaQueryService.getReferenceQuery());
        if (mediaQueryService.getReferenceQuery() != null) {
            return mediaLinksService.multiSearchTmdb(custom, MediaIdentifier.IMDB, mediaQueryService);
        }
        return List.of();
    }


}

package service;

import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import model.links.MediaLinkDto;
import model.links.TvMediaLinkDto;
import model.multipart.MultiPartElement;
import model.multipart.MultipartDto;
import org.springframework.stereotype.Service;
import service.exceptions.NetworkException;
import service.query.MediaQueryService;
import util.MediaIdentity;
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
    public MultipartDto getMultiPartDto(UUID uuid, MediaQueryService mediaQueryService) {
        mediaQueryService.setReferenceQuery(uuid);
        List<MediaQuery> groupedQueries = mediaQueryService.getGroupedQueriesWithId(uuid);
        if (groupedQueries.size() > 1) {
            MultipartDto multipartDto = new MultipartDto();
            multipartDto.setQueryUuid(uuid);
            int counter = 1;
            for (MediaQuery query : groupedQueries) {
//                multipartDto.addMultiPartElement(new MultiPartElement(query.getFilePath()));
                multipartDto.addMultiPartElement(new MultiPartElement(query, counter++));
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
    public List<QueryResult> getMovieResults(MultipartDto multipartDto, MediaQueryService mediaQueryService) throws NetworkException {
        mediaQueryService.addQueriesToProcess(multipartDto.getMultiPartElementList());
        return mediaLinksService.executeMediaQuery("",
                MediaIdentity.IMDB, mediaQueryService);
    }

    @Override
    public List<QueryResult> getMovieResults(MediaQueryService mediaQueryService) throws NetworkException {
        mediaQueryService.addQueryToProcess(mediaQueryService.getReferenceQuery());
        return mediaLinksService.executeMediaQuery("",
                MediaIdentity.IMDB, mediaQueryService);
    }

    @Override
    public List<QueryResult> getResultsCustomSearchTmdb(MediaQueryService mediaQueryService,
                                                        String custom,
                                                        Optional<Integer> year) throws NetworkException {
        mediaQueryService.addQueryToProcess(mediaQueryService.getReferenceQuery());
        return mediaLinksService.searchTmdbWithTitleAndYear(custom,
                MediaIdentity.IMDB,
                year.orElse(0),
                mediaQueryService);
    }

    @Override
    public List<QueryResult> getResultsCustomSearchWeb(MediaQueryService mediaQueryService, String custom) throws NetworkException {
        mediaQueryService.addQueryToProcess(mediaQueryService.getReferenceQuery());
        return mediaLinksService.executeMediaQuery(custom, MediaIdentity.IMDB, mediaQueryService);
    }

    @Override
    public List<QueryResult> getResults(MediaQueryService mediaQueryService) throws NetworkException {
        mediaQueryService.addQueryToProcess(mediaQueryService.getReferenceQuery());
        return mediaLinksService.executeMediaQuery("",
                MediaIdentity.IMDB, mediaQueryService);
    }
}

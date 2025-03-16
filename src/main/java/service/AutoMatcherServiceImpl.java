package service;

import model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import service.exceptions.NetworkException;
import service.query.MediaQueryService;
import util.MediaIdentity;
import util.MediaType;
import util.TextExtractTools;
import websocket.NotificationSender;
import websocket.config.NotificationDispatcher;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
public class AutoMatcherServiceImpl extends NotificationSender<AutoMatcherStatus>
        implements AutoMatcherService {
    private static final Logger LOG = LoggerFactory.getLogger(AutoMatcherServiceImpl.class);
    private static final int REQUEST_DELAY = 500;
    private final RequestService requestService;
    private final ResponseParser responseParser;
    private final MediaLinksService mediaLinksService;
    private final MediaQueryService movieQueryService;
    @Autowired
    private NotificationDispatcher dispatcher;

    public AutoMatcherServiceImpl(RequestService requestService, ResponseParser responseParser,
                                  MediaLinksService mediaLinksService,
                                  @Qualifier("movieQuery") MediaQueryService mediaQueryService) {
        this.requestService = requestService;
        this.responseParser = responseParser;
        this.mediaLinksService = mediaLinksService;
        this.movieQueryService = mediaQueryService;
    }
//                                  PropertiesService propertiesService, MediaLinksService mediaLinksService) {
//        this.mediaLinksService = mediaLinksService;
//        responseParser = ResponseParser.getResponseParser(propertiesService.getNetworkProperties());
//        requestService = RequestService.getRequestService(propertiesService.getNetworkProperties());
//    }

    @Async
    public Future<List<MediaLink>> autoMatchFilesWithFuture() throws NetworkException {
        List<MediaQuery> mediaQueryList = movieQueryService.getCurrentMediaQueries();
        List<MediaLink> mediaLinks = new LinkedList<>();
        AutoMatcherStatus message;
        int index = 0;
        if (mediaQueryList.isEmpty()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
        LOG.info("[ auto ] Elements to process: {}", mediaQueryList.size());
        for (MediaQuery mq : mediaQueryList) {
            message = getMessage(mediaQueryList, mq, index++);
            sendNotification(message);
            if (!movieQueryService.isMultipart(mq)) {
                MediaType type = (TextExtractTools.hasExtrasInName(mq.getFilePath())) ? MediaType.EXTRAS : MediaType.MOVIE;
                mq.setMediaType(type);
                movieQueryService.addQueryToProcess(mq);
                int linksCreationResults = autoMatchSingleFile(Path.of(mq.getFilePath()));
//                linksCreationResults.forEach(lcr -> mediaLinks.add(lcr.getMediaLink()));
//                index = index + linksCreationResults.size();
                // TODO generete final view of all new links, org -> link
            }
        }
        message = getFinalMessage(mediaQueryList, index);

        sendNotification(message);
        return new AsyncResult<>(mediaLinks);
    }

    public int autoMatchSingleFile(Path path) throws NetworkException {
//        List<OperationResult> linksWithBestMatches = List.of();
        DeductedQuery deductedQuery = TextExtractTools.extractTitleAndYear(path.toString());
        if (deductedQuery != null && deductedQuery.getPhrase() != null && deductedQuery.getYear() != 0) {
            List<QueryResult> queryResults = searchWithDeductedQuery(deductedQuery);
            createLinksWithBestMatches(queryResults, deductedQuery);
            try {
                TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY);
            } catch (InterruptedException e) {
                LOG.error("[ auto_matcher ]: {} - {}", path, e.getMessage());
            }
        }
        return 0;
    }

    AutoMatcherStatus getMessage(List<MediaQuery> mediaQueryList, MediaQuery currentQuery, int queryIndex) {
        AutoMatcherStatus autoMatcherStatus = new AutoMatcherStatus();
        int size = mediaQueryList.size();
        String filePath = currentQuery.getFilePath();

        autoMatcherStatus.setCurrentFile(filePath);
        autoMatcherStatus.setEnabled(true);
        autoMatcherStatus.setTotalElements(size);
        autoMatcherStatus.setCurrentElementNumber(queryIndex);
        return autoMatcherStatus;
    }

    AutoMatcherStatus getFinalMessage(List<MediaQuery> mediaQueryList, int queryIndex) {
        AutoMatcherStatus status = new AutoMatcherStatus();
        int size = mediaQueryList.size();
        status.setCurrentFile("Finished! Please, click 'Refresh' button.");
        status.setTotalElements(size);
        status.setCurrentElementNumber(queryIndex);
        status.setEnabled(false);
        return status;
    }


    @Override
    public void sendNotification(AutoMatcherStatus notification) {
        if (this.dispatcher != null) {
            this.dispatcher.dispatch(notification);
        }
    }

    /*
     * Send info about current file
     * */
    void dispatchMessage(List<MediaQuery> mediaQueryList, MediaQuery currentQuery, int queryIndex) {
        int size = mediaQueryList.size();
        String filePath = currentQuery.getFilePath();
        AutoMatcherStatus autoMatcherStatus = new AutoMatcherStatus();
        autoMatcherStatus.setCurrentFile(filePath);
        autoMatcherStatus.setEnabled(true);
        autoMatcherStatus.setTotalElements(size);
        autoMatcherStatus.setCurrentElementNumber(queryIndex);
        dispatcher.dispatch(autoMatcherStatus);
    }

    /*
     * Checks if given path contains phrases that indicate bonus content
     * */
//    boolean hasExtrasInName(String path) {
//        Path of = Path.of(path);
//        String regex = "(?i).+(interview|featurette|deleted)";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(of.toString());
//        return matcher.find();
//    }

//    /*
//     * Checks if given paths contains phrases "sample" or "trailer"
//     * */
//    boolean isSampleOrTrailer(String path) {
//        String regex = "(?i).+(sample|trailer)";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(path);
//        return matcher.find();
//    }

    /*
     * Search for movie with given title and year.
     * Returns query result list.
     * */
    private List<QueryResult> searchWithDeductedQuery(DeductedQuery deductedQuery) throws NetworkException {
        String response = null;
        response = requestService.tmdbApiTitleAndYearMovie(deductedQuery.getPhrase(), deductedQuery.getYear());
        // search results - json
        LOG.info(response);

        return responseParser.parseTmdbApiMovieResults(response, deductedQuery.getPath());
    }

    /*
     * If results list have only one element use it for creating symbolic link.
     * Any file containing special keywords is being marked as extra feature.
     * */
    private List<MediaLink> createLinksWithBestMatches(List<QueryResult> queryResults, DeductedQuery deductedQuery) throws NetworkException {
        if (queryResults.size() == 1 && !TextExtractTools.isSampleOrTrailer(deductedQuery.getPath())) {
//            MediaType type = (hasExtrasInName(deductedQuery.getPath())) ? MediaType.EXTRAS : MediaType.MOVIE;
            return mediaLinksService.createFileLink(queryResults.get(0),
                    MediaIdentity.TMDB,
                    movieQueryService);
        }
        return List.of();
    }

    private String replaceIllegalCharacters(String title) {
        String illegalNames = "[#%&{}\\<>*?/\\[\\]_().$!\"+:@`|=]+";
        Pattern p = Pattern.compile(illegalNames);
        return p.matcher(title).replaceAll(" ");
    }

}

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class AutoMatcherServiceImpl extends NotificationSender<AutoMatcherStatus>
        implements AutoMatcherService {
    private static final Logger LOG = LoggerFactory.getLogger(AutoMatcherServiceImpl.class);
    private static final int REQUEST_DELAY = 500;
    private final RequestService requestService;
    private final ResponseParser responseParser;
    private final MediaLinksService mediaLinksService;
    private final MediaQueryService movieQueryService;
    private List<MediaLink> mediaLinks;
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

    public List<MediaLink> getMediaLinks() {
        return mediaLinks;
    }

    public void setMediaLinks(List<MediaLink> mediaLinks) {
        this.mediaLinks = mediaLinks;
    }

    @Async
    public Future<List<MediaLink>> autoMatchFilesWithFuture() throws NetworkException {
        List<MediaQuery> mediaQueryList = movieQueryService.getCurrentMediaQueries();
        List<MediaLink> mediaLinks = new LinkedList<>();
        int index = 0;
        if (mediaQueryList.isEmpty()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
        LOG.info("[ auto ] Elements to process: {}", mediaQueryList.size());
        for (MediaQuery mq : mediaQueryList) {
            setMatcherStatusAndSend(mediaQueryList.size(), mq.getFilePath(), "", index++);
//            sendNotification(message);
            if (!movieQueryService.isMultipart(mq)) {
                MediaType type = (TextExtractTools.hasExtrasInName(mq.getFilePath()))
                        ? MediaType.EXTRAS
                        : MediaType.MOVIE;
                mq.setMediaType(type);
                movieQueryService.addQueryToProcess(mq);
                autoMatchSingleFile(Path.of(mq.getFilePath()));
            }
        }
        setMatcherFinalMessageAndSend(mediaQueryList.size(), index);
        return new AsyncResult<>(mediaLinks);
    }

    // TODO this sould be async as well as the function to create links
    @Async
    public Future<List<MediaLink>> autoMatchAndGetLinks() throws NetworkException {
        List<MediaQuery> mediaQueryList = movieQueryService.getCurrentMediaQueries();
        mediaLinks = new LinkedList<>();
        int index = 0;
        if (mediaQueryList.isEmpty()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
        for (MediaQuery mq : mediaQueryList) {
            setMatcherStatusAndSend(
                    mediaQueryList.size(),
                    mq.getFilePath(),
                    "",
                    index++
            );
            if (!movieQueryService.isMultipart(mq)) {
                MediaType type = (TextExtractTools.hasExtrasInName(mq.getFilePath()))
                        ? MediaType.EXTRAS
                        : MediaType.MOVIE;
                mq.setMediaType(type);
                movieQueryService.addQueryToProcess(mq);
                List<MediaLink> links = autoMatchSingleFile(Path.of(mq.getFilePath()));
                if (!links.isEmpty()) {
                    setMatcherStatusAndSend(
                            mediaQueryList.size(),
                            links.get(0).getOriginalPath(),
                            links.get(0).getLinkPath(),
                            index
                    );
                    mediaLinks.addAll(links);
                }
            }
        }
        LOG.info("[ auto_matcher ] Created links for {} elements", mediaLinks.size());
        setMatcherFinalMessageAndSend(
                mediaQueryList.size(),
                index
        );
        mediaLinksService.setMediaLinksToProcess(mediaLinks);
        return CompletableFuture.completedFuture(mediaLinks);
    }

    public List<MediaLink> autoMatchSingleFile(Path path) throws NetworkException {
        DeductedQuery deductedQuery = TextExtractTools.extractTitleAndYear(path.toString());
        if (deductedQuery != null && deductedQuery.getPhrase() != null && deductedQuery.getYear() != 0) {
            List<QueryResult> queryResults = searchWithDeductedQuery(deductedQuery);
            try {
                TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY);
            } catch (InterruptedException e) {
                LOG.error("[ auto_matcher ]: {} - {}", path, e.getMessage());
            }
            return createLinksWithBestMatches(queryResults);
        }
        return List.of();
    }

    void setMatcherStatusAndSend(int size, String message, String link, int queryIndex) {
        AutoMatcherStatus autoMatcherStatus = new AutoMatcherStatus();
        autoMatcherStatus.setLink(link);
        autoMatcherStatus.setCurrentFile(message);
        autoMatcherStatus.setEnabled(true);
        autoMatcherStatus.setType(AutoMatcherStatusType.RUNNING);
        autoMatcherStatus.setTotalElements(size);
        autoMatcherStatus.setCurrentElementNumber(queryIndex);
        sendNotification(autoMatcherStatus);
    }

    void setMatcherFinalMessageAndSend(int size, int queryIndex) {
        AutoMatcherStatus status = new AutoMatcherStatus();
        status.setCurrentFile("Done!");
        status.setTotalElements(size);
        status.setCurrentElementNumber(queryIndex);
        status.setEnabled(false);
        status.setType(AutoMatcherStatusType.FINISHED);
        sendNotification(status);
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
     * Search for movie with given title and year.
     * Returns query result list.
     * */
    private List<QueryResult> searchWithDeductedQuery(DeductedQuery deductedQuery) throws NetworkException {
        String response = null;
        response = (deductedQuery.getYear() != 1000)
                ? requestService.tmdbApiTitleAndYearMovie(deductedQuery.getPhrase(), deductedQuery.getYear())
                : requestService.tmdbApiTitleMovie(deductedQuery.getPhrase());
        LOG.info(response);
        return responseParser.parseTmdbApiMovieResults(response, deductedQuery.getPath());
    }

    /*
     * If results list have only one element use it for creating symbolic link.
     * Any file containing special keywords is being marked as extra feature.
     * */
    private List<MediaLink> createLinksWithBestMatches(
            List<QueryResult> queryResults) throws NetworkException {
        if (queryResults.size() == 1) {
            LOG.info("[ auto_matcher ] creating link for: {}", queryResults.get(0).getOriginalPath());
            return mediaLinksService.createFileLink(
                    queryResults.get(0),
                    MediaIdentity.TMDB,
                    movieQueryService
            );
        }
        return List.of();
    }

}

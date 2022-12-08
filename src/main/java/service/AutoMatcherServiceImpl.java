package service;

import model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import util.MediaIdentity;
import util.MediaType;
import websocket.NotificationSender;
import websocket.config.NotificationDispatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AutoMatcherServiceImpl extends NotificationSender<AutoMatcherStatus> implements AutoMatcherService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoMatcherServiceImpl.class);
    private static final int REQUEST_WAIT = 500;

    private final RequestService requestService;
    private final ResponseParser responseParser;
    @Autowired
    private MediaLinksService mediaLinksService;

    @Autowired
    private MediaQueryService mediaQueryService;

    @Autowired
    private NotificationDispatcher dispatcher;

//    public AutoMatcherServiceImpl(PropertiesService propertiesService, MediaLinksService mediaLinksService) {
//        this.mediaLinksService = mediaLinksService;
//        responseParser = ResponseParser.getResponseParser(propertiesService.getNetworkProperties());
//        requestService = RequestService.getRequestService(propertiesService.getNetworkProperties());
//    }


    public AutoMatcherServiceImpl(PropertiesService propertiesService, MediaLinksService mediaLinksService) {
//        this.mediaLinksService = mediaLinksService;
        responseParser = ResponseParser.getResponseParser(propertiesService.getNetworkProperties());
        requestService = RequestService.getRequestService(propertiesService.getNetworkProperties());
    }

    @Async
    public Future<List<MediaLink>> autoMatchFilesWithFuture() {
        List<MediaQuery> mediaQueryList = mediaLinksService.getMediaQueryList();
        List<MediaLink> mediaLinks = new LinkedList<>();
        AutoMatcherStatus message;
        int index = 0;
        if (mediaQueryList.isEmpty()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
        for (MediaQuery mq : mediaQueryList) {
            message = getMessage(mediaQueryList, mq, index++);
            sendNotification(message);
            if (!mediaLinksService.isMultipart(mq)) {
                MediaType type = (hasExtrasInName(mq.getFilePath())) ? MediaType.EXTRAS : MediaType.MOVIE;
                mq.setMediaType(type);
                mediaQueryService.addQueryToProcess(mq);
                List<LinkCreationResult> linksCreationResults = autoMatchSingleFile(Path.of(mq.getFilePath()));
                linksCreationResults.forEach(lcr -> mediaLinks.add(lcr.getMediaLink()));
//                index = index + linksCreationResults.size();
            }
        }
        message = getFinalMessage(mediaQueryList, index);
        sendNotification(message);
        return new AsyncResult<>(mediaLinks);
    }

    public List<LinkCreationResult> autoMatchSingleFile(Path path) {
        List<LinkCreationResult> linksWithBestMatches = null;
        DeductedQuery deductedQuery = extractTitleAndYear(path.toString());
        if (deductedQuery != null && deductedQuery.getPhrase() != null && deductedQuery.getYear() != null) {
            List<QueryResult> queryResults = searchWithDeductedQuery(deductedQuery);
            linksWithBestMatches = createLinksWithBestMatches(queryResults, deductedQuery);
            try {
                TimeUnit.MILLISECONDS.sleep(REQUEST_WAIT);
            } catch (InterruptedException e) {
                LOG.error("[ auto_matcher ]: {} - {}", path, e.getMessage());
            }
        }
        return linksWithBestMatches;
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
    boolean hasExtrasInName(String path) {
        Path of = Path.of(path);
        String regex = "(?i).+(interview|featurette|deleted)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(of.toString());
        return matcher.find();
    }

    /*
     * Checks if given paths contains phrases "sample" or "trailer"
     * */
    boolean isSampleOrTrailer(String path) {
        Path of = Path.of(path);
        String regex = "(?i).+(sample|trailer)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(of.toString());
        return matcher.find();
    }

    /*
     * Extract movie title and production year from given path.
     * Files containing keyword sample or trailer are being ignored.
     * */
    @Override
    public DeductedQuery extractTitleAndYear(String path) {
        Path of = Path.of(path);
        Path fileName = of.getName(of.getNameCount() - 1);
        if (isSampleOrTrailer(path)) return null;
        String regex = "\\b^.+?\\d{4}\\b";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(fileName.toString());
        if (m.find()) {
            String group = m.group();
            String filtered = replaceIllegalCharacters(group);
            int i = filtered.length() - 4;
            String year = filtered.substring(i);
            String title = filtered.substring(0, i).trim();
            return new DeductedQuery(title, year, path);
        }
        return null;
    }

    /*
     * Search for movie with given title and year.
     * Returns query result list.
     * */
    private List<QueryResult> searchWithDeductedQuery(DeductedQuery deductedQuery) {
        String response = null;
        try {
            response = requestService.tmdbApiTitleAndYear(deductedQuery);
        } catch (IOException e) {
            LOG.error("[ auto_match ] Response error: {}", e.getMessage());
        }
        // search results - json
        LOG.info(response);

        return responseParser.parseTmdbApiSearchResults(response, Path.of(deductedQuery.getPath()));
    }

    /*
     * If results list have only one element use it for creating symbolic link.
     * Any file containing special keywords is being marked as extra feature.
     * */
    private List<LinkCreationResult> createLinksWithBestMatches(List<QueryResult> queryResults, DeductedQuery deductedQuery) {
        if (queryResults.size() == 1 && !isSampleOrTrailer(deductedQuery.getPath())) {
//            MediaType type = (hasExtrasInName(deductedQuery.getPath())) ? MediaType.EXTRAS : MediaType.MOVIE;
            return mediaLinksService.createFileLink(queryResults.get(0), MediaIdentity.TMDB);
        }
        return List.of();
    }

    private String replaceIllegalCharacters(String title) {
        String illegalNames = "[#%&{}\\<>*?/\\[\\]_().$!\"+:@`|=]+";
        Pattern p = Pattern.compile(illegalNames);
        return p.matcher(title).replaceAll(" ");
    }

}

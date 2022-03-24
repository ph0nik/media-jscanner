package service;


import model.DeductedQuery;
import model.MediaQuery;
import model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import util.MediaIdentity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AutoMatcherServiceImpl implements AutoMatcherService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoMatcherServiceImpl.class);
    private static final int REQUEST_WAIT = 1;

    private final RequestService requestService;
    private final Properties networkProperties;
    private final ResponseParser responseParser;
    private final MediaLinksService mediaLinksService;

    public AutoMatcherServiceImpl(PropertiesService propertiesService, MediaLinksService mediaLinksService) {
        this.networkProperties = propertiesService.getNetworkProperties();
        this.mediaLinksService = mediaLinksService;
        responseParser = new ResponseParser(networkProperties);
        requestService = new RequestService(networkProperties);
    }

    @Async
    public void autoMatchFiles() {
        List<MediaQuery> mediaQueryList = mediaLinksService.getMediaQueryList();
        int x = 5;
        int i = 0;
        for (MediaQuery mq : mediaQueryList) {
            if (i == x) break;
            DeductedQuery deductedQuery = extractTitleAndYear(mq.getFilePath());
            if (deductedQuery != null && deductedQuery.getPhrase() != null && deductedQuery.getYear() != null) {
                List<QueryResult> queryResults = searchWithDeductedQuery(deductedQuery);
                createLinksWithBestMatches(queryResults);
                try {
                    TimeUnit.SECONDS.sleep(REQUEST_WAIT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
    }

    @Async
    public Future<Boolean> autoMatchFilesWithFuture() {
        List<MediaQuery> mediaQueryList = mediaLinksService.getMediaQueryList();
        int x = 5;
        int i = 0;
        for (MediaQuery mq : mediaQueryList) {
            if (i == x) break;
            DeductedQuery deductedQuery = extractTitleAndYear(mq.getFilePath());
            if (deductedQuery != null && deductedQuery.getPhrase() != null && deductedQuery.getYear() != null) {
                List<QueryResult> queryResults = searchWithDeductedQuery(deductedQuery);
                createLinksWithBestMatches(queryResults);
                try {
                    TimeUnit.SECONDS.sleep(REQUEST_WAIT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
        return new AsyncResult<Boolean>(true);
    }
    /*
    * Extract movie title and production year from given path.
    *
    * */
    @Override
    public DeductedQuery extractTitleAndYear(String path) {
        Path of = Path.of(path);
        Path fileName = of.getName(of.getNameCount() - 1);
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
    public List<QueryResult> searchWithDeductedQuery(DeductedQuery deductedQuery) {
        String response = null;
        try {
            response = requestService.tmdbApiTitleAndYear(deductedQuery);
        } catch (IOException e) {
            LOG.error("[ auto_match ] Response error: {}", e.getMessage());
        }
        LOG.info(response);

        return responseParser.parseTmdbApiSearchResults(response, Path.of(deductedQuery.getPath()));
    }

    /*
    * If results list have only one element use it for creating symbolic link.
    * */
    public void createLinksWithBestMatches(List<QueryResult> queryResults) {
        if (queryResults.size() == 1) {
            mediaLinksService.createSymLink(queryResults.get(0), MediaIdentity.TMDB);
        }
    }

    private String replaceIllegalCharacters(String title) {
        String illegalNames = "[#%&{}\\<>*?/\\[\\]_().$!\"+:@`|=]+";
        Pattern p = Pattern.compile(illegalNames);
        return p.matcher(title).replaceAll(" ");
    }
}

package service;

import model.DeductedQuery;
import model.QueryResult;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import service.exceptions.NetworkException;
import util.MediaIdentity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;

@Component
public class RequestService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestService.class);
    private PropertiesService propertiesService;

    public RequestService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    String tmdbMultiSearch(DeductedQuery deductedQuery) throws NetworkException {
        LOG.info("[ tmdb_multisearch ] Creating request for multisearch...");
        String apiRequest = propertiesService.getNetworkProperties().getProperty("tmdb_multisearch")
                .replace("<<query>>", deductedQuery.getPhrase());
        LOG.info("[ tmdb_multisearch ] {}", apiRequest);
        return tmdbApiGeneralRequest(apiRequest);
    }

    String tmdbApiTitleAndYearMovie(String query, int year) throws NetworkException {
        LOG.info("[ request_service ] Creating request for title and year movie search...");
        String apiRequest = propertiesService.getNetworkProperties().getProperty("tmdb_movie_title_year")
                .replace("<<query>>", query)
                .replace("<<year>>", String.valueOf(year));
        LOG.info("[ request_service ] {}", apiRequest);
        return tmdbApiGeneralRequest(apiRequest);
    }

    String tmdbApiTitleAndYearTv(String query, int year) throws NetworkException {
        LOG.info("[ request_service ] Creating request for title and year tv search...");
        String apiRequest = propertiesService.getNetworkProperties().getProperty("tmdb_tv_search_year")
                .replace("<<query>>", query)
                .replace("<<year>>", String.valueOf(year));
        LOG.info("[ request_service ] {}", apiRequest);
        return tmdbApiGeneralRequest(apiRequest);
    }


    /*
     * Generate search query with given phrase and media identity.
     * */
    private String generateQuery(String phrase) {
        phrase = phrase.replaceAll("-", " ");
        return propertiesService.getNetworkProperties().getProperty("imdb_web_search")
                .replace("<<query>>", phrase);
    }

    String webSearchRequest(String query) throws IOException {
        String queryFormatted = generateQuery(query);
        LOG.info("[ request_service ] web search query: {}", queryFormatted);
        // POST connection - redirect fails
//        Connection.Response post = Jsoup.connect(linkerProperties.getProperty("search_url_post"))
//                .data("query", queryFormatted)
//                .userAgent(linkerProperties.getProperty("User-Agent"))
//                .followRedirects(false)
//                .timeout(3000)
//                .execute();
        // GET connection
        Connection.Response response = Jsoup.connect(queryFormatted)
                .userAgent(propertiesService.getNetworkProperties().getProperty("user_agent"))
                .referrer(propertiesService.getNetworkProperties().getProperty("referer"))
                .header("origin", propertiesService.getNetworkProperties().getProperty("origin"))
                .ignoreHttpErrors(true) // try with ignore
                .timeout(3000)
                .execute();
        LOG.info("[ request_service ] web search: {}", response.statusCode());
        return response.body();
    }

    /*
     * TheMovieDB API request, returns json object as string.
     * */
    String tmdbApiRequestWithSpecifiedId(QueryResult queryResult, MediaIdentity mediaIdentity) throws NetworkException {
        if (queryResult == null) {
            LOG.error("[ request_service ] tmdbApiRequest error, query result is null");
            return "";
        }
        if (queryResult.getImdbId() == null && queryResult.getTheMovieDbId() <= 0) {
            LOG.error("[ request_service ] tmdbApiRequest error, " +
                            "no identifier found; getImdbId() = {}, getTheMovieDbId() = {}",
                    queryResult.getImdbId(), queryResult.getTheMovieDbId());
            return "";
        }
        String apiRequest = "";
        if (mediaIdentity == MediaIdentity.IMDB) {
            apiRequest = propertiesService.getNetworkProperties().getProperty("tmdb_search_with_imdb")
                    .replace("<<imdb_id>>", queryResult.getImdbId());
        }
        if (mediaIdentity == MediaIdentity.TMDB) {
            apiRequest = propertiesService.getNetworkProperties().getProperty("tmdb_search_with_tmdb")
                    .replace("<<tmdb_id>>", Integer.toString(queryResult.getTheMovieDbId()));
        }
        return tmdbApiGeneralRequest(apiRequest);
    }

    /*
    * Get season details with given season number
    * */
    String tmdbGetSeasonDetails(QueryResult queryResult) throws NetworkException {
        String apiRequest = propertiesService.getNetworkProperties().getProperty("tmdb_tv_get_details")
                .replace("<<tmdb_id>>", Integer.toString(queryResult.getTheMovieDbId()));
        return tmdbApiGeneralRequest(apiRequest);
    }

    public String tmdbApiTvRequest(String query) throws NetworkException {
        String apiRequest = propertiesService.getNetworkProperties().getProperty("tmdb_tv_search_title")
                .replace("<<query>>", query);
        return tmdbApiGeneralRequest(apiRequest);
    }

    private String tmdbApiGeneralRequest(String apiRequest) throws NetworkException {
        Connection.Response response = null;
        try {
            response = Jsoup.connect(apiRequest)
                    .userAgent(propertiesService.getNetworkProperties().getProperty("user_agent"))
                    .header("Authorization",
                            "Bearer " + propertiesService.getNetworkProperties().getProperty("api_key_v4"))
                    .ignoreContentType(true)
                    .timeout(3000)
                    .execute();
        } catch (MalformedURLException e) {
            throw new NetworkException("Malformed url: " + e.getMessage());
        } catch (HttpStatusException e) {
            throw new NetworkException(e.getStatusCode() + " | " + e.getUrl());
        } catch (UnsupportedMimeTypeException e) {
            throw new NetworkException(e.getMessage() + " | " + e.getMimeType() + " | " + e.getUrl());
        } catch (SocketTimeoutException e) {
            throw new NetworkException("Time out: " + e.getMessage());
        } catch (IOException e) {
            throw new NetworkException(e.getMessage());
        }
        LOG.info("[ request_service ] tmdbApiRequest: {}", response.statusCode());
        return response.body();
    }


}

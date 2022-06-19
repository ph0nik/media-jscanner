package service;

import model.DeductedQuery;
import model.QueryResult;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.MediaIdentity;

import java.io.IOException;
import java.util.Properties;

class RequestService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestService.class);

    private final Properties networkProperties;

    private RequestService(Properties networkProperties) {
        this.networkProperties = networkProperties;
    }

    static RequestService getRequestService(Properties networkProperties) {
        return new RequestService(networkProperties);
    }

    /*
     * Search query executed via themoviedb api
     * */
    String tmdbApiSearchRequest(String query) throws IOException {
        LOG.info("[ request_service ] api search query: {}", query);
        String apiRequest =
                networkProperties.getProperty("tmdb_api3") +
                        networkProperties.getProperty("tmdb_movie_search") +
                        networkProperties.getProperty("tmdb_request_lang") +
                        networkProperties.getProperty("tmdb_movie_search_query") +
                        query;
        return tmdbApiGeneralRequest(apiRequest);
    }

    String tmdbApiTitleAndYear(DeductedQuery deductedQuery) throws IOException {
        LOG.info("[ request service ] creating deducted query");
        String apiRequest =
                networkProperties.getProperty("tmdb_api3") +
                        networkProperties.getProperty("tmdb_movie_search") +
                        networkProperties.getProperty("tmdb_request_lang") +
                        networkProperties.getProperty("tmdb_movie_search_query") +
                        deductedQuery.getPhrase() +
                        networkProperties.getProperty("tmdb_movie_search_year") +
                        deductedQuery.getYear();
        return tmdbApiGeneralRequest(apiRequest);
    }

    /*
     * Generate search query with given phrase and media identity.
     * */
    private String generateQuery(String phrase) {
        phrase = phrase.replaceAll("-", " ");
            return networkProperties.getProperty("imdb_pre_query") +
                    phrase +
                    " " +
                    networkProperties.getProperty("imdb_post_query") +
                    networkProperties.getProperty("imdb_query_options");
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
                .userAgent(networkProperties.getProperty("user_agent"))
                .referrer(networkProperties.getProperty("referer"))
                .header("origin", networkProperties.getProperty("origin"))
                .ignoreHttpErrors(true) // try with ignore
                .timeout(3000)
                .execute();
        LOG.info("[ request_service ] web search: {}", response.statusCode());
        return response.body();
    }

    /*
     * TheMovieDB API request, returns json object as string.
     * */
    String tmdbApiRequestWithSpecifiedId(QueryResult queryResult, MediaIdentity mediaIdentity) throws IOException {
        if (queryResult == null) {
            LOG.error("[ request_service ] tmdbApiRequest error, query result is null");
            return "";
        } else if (queryResult.getImdbId() == null && queryResult.getTheMovieDbId() <= 0) {
            LOG.error("[ request_service ] tmdbApiRequest error, no identifier found; getImdbId() = {}, getTheMovieDbId() = {}",
                    queryResult.getImdbId(), queryResult.getTheMovieDbId());
            return "";
        }
        String apiRequest = "";
        if (mediaIdentity.equals(MediaIdentity.IMDB)) {
            apiRequest = networkProperties.getProperty("tmdb_api3") +
                    networkProperties.getProperty("tmdb_external_id") +
                    queryResult.getImdbId() +
                    networkProperties.getProperty("tmdb_request_lang") +
                    networkProperties.getProperty("tmdb_external_site");
        }
        if (mediaIdentity.equals(MediaIdentity.TMDB)) {
            apiRequest = networkProperties.getProperty("tmdb_api3") +
                    networkProperties.getProperty("tmdb_movie_category") +
                    queryResult.getTheMovieDbId() +
                    networkProperties.getProperty("tmdb_request_lang");
        }
        return tmdbApiGeneralRequest(apiRequest);
    }

    private String tmdbApiGeneralRequest(String apiRequest) throws IOException {
        Connection.Response response = Jsoup.connect(apiRequest)
                .userAgent(networkProperties.getProperty("user_agent"))
                .header("Authorization", "Bearer " + networkProperties.getProperty("api_key_v4"))
                .ignoreContentType(true)
                .timeout(3000)
                .execute();
        LOG.info("[ request_service ] tmdbApiRequest: {}", response.statusCode());
        return response.body();
    }
}

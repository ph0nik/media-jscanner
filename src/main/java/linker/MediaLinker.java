package linker;

import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;

import java.util.List;

public interface MediaLinker {

    /*
     * Get list of all queries from db
     * */
    List<MediaQuery> mediaQueryList();

    /*
     * For a given query perform online search for matching elements within given domain.
     * If first parameter is not empty it's going to be used as search phrase,
     * otherwise query from MediaQuery object is being used.
     * Returns List of QueryResult or null in case of exception.
     * */
    List<QueryResult> executeMediaQuery(String customQuery, MediaQuery mediaQuery);

    /*
     * Create symlink with specified query result and link properties
     * */
    MediaLink createSymLink(QueryResult queryResult);


}

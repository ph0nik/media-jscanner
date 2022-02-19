package linker;

import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;

import java.nio.file.Path;
import java.util.List;

public interface MediaLinker {

    /*
    * Get list of all queries from db
    * */
    List<MediaQuery> mediaQueryList();

    /*
    * For a given query perform online search for matching elements within given domain
    * */
    List<QueryResult> executeQuery(MediaQuery mediaQuery);

    /*
    * Create symlink with specified query result and link properties
    * */
    MediaLink createSymLink(QueryResult queryResult);

    /*
    * Parse server response and extract query results, return as list
    * */
    List<QueryResult> parseReturn(String document, Path filePath);



}

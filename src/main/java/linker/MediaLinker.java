package linker;

import model.QueryResult;
import model.MediaQuery;

import java.util.List;

public interface MediaLinker {

    List<MediaQuery> mediaQueryList();

    List<QueryResult> executeQuery(MediaQuery mediaQuery);

    boolean createSymLink(QueryResult queryResult);


}

package dao;

import model.MediaLink;
import model.MediaQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest(classes = dao.MediaTrackerDaoImpl.class)
//@RunWith(SpringRunner.class)
class MediaTrackerDaoImplTest {

    @Autowired
    MediaTrackerDao dao;
    MediaQuery mediaQuery1;
    MediaQuery mediaQuery2;
    String filePath = "g:\\root\\observed\\movie_title1\\movie_file1.mkv";
    String filePath2 = "g:\\root\\observed\\movie_title2\\movie_file2.avi";

    @BeforeEach
    void initDao() {
        dao = new MediaTrackerDaoImpl();
    }

    @BeforeEach
    void setMediaQuery() {
        mediaQuery1 = new MediaQuery();
        mediaQuery1.setFilePath(filePath);
        mediaQuery2 = new MediaQuery();
        mediaQuery2.setFilePath(filePath2);
    }


    @BeforeEach
    void openFactory() {
        MediaEntityManager.getEntityManagerFactory();
    }

    @AfterEach
    void closeFactory() {
        MediaEntityManager.shutdown();
    }

    @Test
    void addQueryToQueue() {
        List<MediaQuery> allMediaQueries = dao.getAllMediaQueries();
        assertEquals(0, allMediaQueries.size());
        dao.addQueryToQueue(mediaQuery1);
        allMediaQueries = dao.getAllMediaQueries();
        assertEquals(1, allMediaQueries.size());
        MediaQuery mediaQuery2 = dao.findQueryByFilePath(filePath);
        assertEquals(mediaQuery1, mediaQuery2);
    }

    @Test
    void addTheSameElementMultipleTimes() {
        dao.addQueryToQueue(mediaQuery1);
        MediaQuery mediaQuery = new MediaQuery();
        mediaQuery.setFilePath(filePath);
        dao.addQueryToQueue(mediaQuery);
        List<MediaQuery> allMediaQueries = dao.getAllMediaQueries();
        assertEquals(1, allMediaQueries.size());
    }

    @Test
    void removeQueryFromQueue() {
        dao.addQueryToQueue(mediaQuery1);
        List<MediaQuery> allMediaQueries = dao.getAllMediaQueries();
        assertEquals(1, allMediaQueries.size());
        dao.removeQueryFromQueue(mediaQuery1);
        allMediaQueries = dao.getAllMediaQueries();
        assertEquals(0, allMediaQueries.size());
    }

    @Test
    void removeNonExistentQueryFromQueue() {
        dao.addQueryToQueue(mediaQuery2);
        List<MediaQuery> allMediaQueries = dao.getAllMediaQueries();
        assertEquals(1, allMediaQueries.size());
        dao.removeQueryFromQueue(mediaQuery1);
        allMediaQueries = dao.getAllMediaQueries();
        assertEquals(1, allMediaQueries.size());
    }

    @Test
    void findQueryByFilePath() {
        dao.addQueryToQueue(mediaQuery1);
        MediaQuery queryByFilePath = dao.findQueryByFilePath(filePath2);
        assertNotEquals(mediaQuery1, queryByFilePath);
        queryByFilePath = dao.findQueryByFilePath(filePath);
        assertEquals(mediaQuery1, queryByFilePath);
    }

    @Test
    void getAllMediaQueries() {
        dao.addQueryToQueue(mediaQuery1);
        dao.addQueryToQueue(mediaQuery2);
        List<MediaQuery> allMediaQueries = dao.getAllMediaQueries();
        assertEquals(2, allMediaQueries.size());
    }

    @Test
    void getQueryById() {
        dao.addQueryToQueue(mediaQuery1);
        dao.addQueryToQueue(mediaQuery2);
        assertEquals(mediaQuery1, dao.getQueryById(1L));
    }

    @Test
    void findInFilePathQuery() {
        dao.addQueryToQueue(mediaQuery1);
        Path testPath2 = Path.of(filePath2);
        String name2 = testPath2.getName(testPath2.getNameCount() - 1).toString();
        List<MediaQuery> inFilePathQuery = dao.findInFilePathQuery(name2);
        assertEquals(0, inFilePathQuery.size());
        Path testPath1 = Path.of(filePath);
        String name1 = testPath1.getName(testPath1.getNameCount() - 1).toString();
        inFilePathQuery = dao.findInFilePathQuery(name1);
        assertEquals(1, inFilePathQuery.size());
    }

    @Test
    void findOtherPath() {
        MediaLink mediaLink = new MediaLink();
        mediaLink.setTargetPath("E:\\Filmy SD\\About Faces  1941.avi");
        mediaLink.setImdbId("tt0401151");
        mediaLink.setTheMovieDbId(442942);
        mediaLink.setLinkPath("G:\\Temp\\jscanner-test-folder\\About Faces (1941) [imdbid-tt0401151]\\About Faces.avi");


        dao.addNewLink(mediaLink);

        String query = "G:\\Temp\\jscanner-test-folder\\About Faces (1941) [imdbid-tt0401151]\\";

        List<MediaLink> inFilePathLink = dao.findInLinkFilePathLink(query);
        assertTrue(inFilePathLink.size() > 0);


    }
}
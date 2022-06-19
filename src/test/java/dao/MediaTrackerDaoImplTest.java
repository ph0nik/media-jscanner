package dao;

import dao.config.HibernateConfigDev;
import model.MediaIgnored;
import model.MediaLink;
import model.MediaQuery;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {SpringHibernateBootstrapDao.class, HibernateConfigDev.class})
@Transactional
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
class MediaTrackerDaoImplTest {

    @Autowired
    @Qualifier("spring")
    MediaTrackerDao dao;
    MediaQuery mediaQuery1;
    MediaQuery mediaQuery2;
    String filePath = "g:\\root\\observed\\movie_title1\\movie_file1.mkv";
    String filePath2 = "g:\\root\\observed\\movie_title2\\movie_file2.avi";
    String testPersistenceUnit = "jscanner-sqlite-test";

//    @BeforeEach
//    void initDao() {
//        dao = new MediaTrackerDaoImpl();
//    }

    @BeforeTransaction
    void setMediaQuery() {
        mediaQuery1 = new MediaQuery();
        mediaQuery1.setFilePath(filePath);
        mediaQuery2 = new MediaQuery();
        mediaQuery2.setFilePath(filePath2);
    }


//    @BeforeEach
//    void openFactory() {
//        MediaEntityManager.getEntityManagerFactory();
//    }
//
//    @AfterEach
//    void closeFactory() {
//        MediaEntityManager.shutdown();
//    }

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
        assertThrows(ConstraintViolationException.class, () -> dao.addQueryToQueue(mediaQuery));
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
        Exception illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> dao.removeQueryFromQueue(mediaQuery1));
        assertEquals("attempt to create delete event with null entity", illegalArgumentException.getMessage());

    }

    @Test
    void findQueryByFilePath() {
        dao.addQueryToQueue(mediaQuery1);
        Exception exception = assertThrows(NoResultException.class, () -> dao.findQueryByFilePath(filePath2));
        assertEquals("No entity found for query", exception.getMessage());
        MediaQuery queryByFilePath = dao.findQueryByFilePath(filePath);
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
        assertEquals(mediaQuery1, dao.getQueryById(mediaQuery1.getQueryId()));
    }

    @Test
    void findInFilePathQuery() {
        dao.addQueryToQueue(mediaQuery1);
        Path testPath2 = Path.of(filePath2);
        String name2 = testPath2.getName(testPath2.getNameCount() - 1).toString();
        List<MediaQuery> inFilePathQuery = dao.findInFilePathQuery(name2);
        assertEquals(0, inFilePathQuery.size());
        Path testPath1 = Path.of(filePath);
        System.out.println(testPath1);

        String name1 = testPath1.getName(testPath1.getNameCount() - 1).toString();

        inFilePathQuery = dao.findInFilePathQuery(testPath1.toString());
        assertEquals(1, inFilePathQuery.size());

        inFilePathQuery = dao.findInFilePathQuery("root\\observed\\movie_title1");
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

        List<MediaLink> inFilePathLink = dao.findInLinkPathLink(query);
        assertTrue(inFilePathLink.size() > 0);
    }


    @Test
    void addIgnoreElement() {
        String path = "E:\\Filmy HD\\!_HD-UHD_Benchmark\\1080p24-hi41-4f-33mbps.mkv";
        MediaIgnored mediaIgnored = new MediaIgnored();
        mediaIgnored.setTargetPath(path);
        dao.addMediaIgnored(mediaIgnored);

        List<MediaIgnored> allMediaIgnored = dao.getAllMediaIgnored();
        assertEquals(1, allMediaIgnored.size());

        MediaIgnored mediaIgnoredByFilePath = dao.findMediaIgnoredByTargetPath(path);
        assertNotNull(mediaIgnoredByFilePath);

        dao.removeMediaIgnored(mediaIgnoredByFilePath.getMediaId());
        allMediaIgnored = dao.getAllMediaIgnored();
        assertEquals(0, allMediaIgnored.size());
    }
}
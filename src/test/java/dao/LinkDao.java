package dao;

import dao.config.HibernateConfigDev;
import model.MediaLink;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {SpringHibernateBootstrapDao.class, HibernateConfigDev.class})
@Transactional
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
public class LinkDao {

    @Autowired
    @Qualifier("spring")
    private MediaTrackerDao mediaTrackerDao;

    private MediaLink mediaLink1;
    private MediaLink mediaLink2;

    @BeforeTransaction
    public void createTestLinks() {
        mediaLink1 = new MediaLink();
        mediaLink1.setLinkPath("some path");
        mediaLink1.setTheMovieDbId(1234);
        mediaLink1.setTargetPath("target path");
        mediaLink1.setImdbId("imdb id");
    }

    @Test
    public void insertNewLink() {
        mediaTrackerDao.addNewLink(mediaLink1);
        List<MediaLink> allMediaLinks = mediaTrackerDao.getAllMediaLinks();
        assertEquals(1, allMediaLinks.size());
        allMediaLinks.forEach(System.out::println);

        List<MediaLink> path = mediaTrackerDao.findInTargetPathLink("get pa");
        assertEquals(1, path.size());
    }

    @Test
    public void deleteNonExistentElement() {
        MediaLink mediaLink = mediaTrackerDao.removeLink(mediaLink1.getMediaId());
        assertNull(mediaLink);
    }

    @Test
    public void deleteSingleElement() {
        mediaTrackerDao.addNewLink(mediaLink1);
        MediaLink mediaLink = mediaTrackerDao.removeLink(mediaLink1.getMediaId());
        assertNotNull(mediaLink);

    }
}

package dao;

import model.MediaLink;
import model.MediaQuery;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.util.List;

public class MediaTrackerDaoImpl implements MediaTrackerDao {

    private static EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();

    public MediaTrackerDaoImpl() {}

    @Override
    public void addQueryToQueue(MediaQuery query) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.persist(query);
        transaction.commit();
    }

    @Override
    public void removeQueryFromQueue(MediaQuery query) {

    }

    @Override
    public MediaQuery findQueryByFilePath(String filePath) {
        TypedQuery<MediaQuery> typedQuery = entityManager.createQuery("SELCT q FROM MediaQuery q WHERE q.file_path=:filepath", MediaQuery.class);
        typedQuery.setParameter("filepath", filePath);
        return typedQuery.getSingleResult();
    }


    @Override
    public List<MediaQuery> getAllMediaQueries() {

        return null;
    }

    @Override
    public void addNewLink(MediaLink mediaLInk) {

    }

    @Override
    public void removeLink(MediaLink mediaLInk) {

    }

    @Override
    public List<MediaLink> getAllMediaLinks() {
        return null;
    }

    @Override
    public MediaLink findMediaLinkByFilePath(String filePath) {
        return null;
    }

}

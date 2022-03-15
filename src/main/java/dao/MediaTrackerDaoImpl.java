package dao;

import model.MediaLink;
import model.MediaQuery;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.nio.file.Path;
import java.util.List;


@Component
public class MediaTrackerDaoImpl implements MediaTrackerDao {

    private static final Logger LOG = LoggerFactory.getLogger(MediaTrackerDaoImpl.class);

//    private static EntityManager entityManager;
//
//    public MediaTrackerDaoImpl() {
//        entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
//    }

    @Override
    public void addQueryToQueue(MediaQuery query) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.persist(query);
            transaction.commit();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            while ((cause != null) && !(cause instanceof ConstraintViolationException)) {
                cause = cause.getCause();
            }
            if (cause != null) {
                LOG.error("[ dao ] " + cause.getMessage());
            }
            if (transaction != null) transaction.rollback();
            LOG.error("[ dao ] " + e.getMessage());
        } finally {
            entityManager.close();
        }
    }

    @Override
    public void removeQueryFromQueue(MediaQuery mediaQuery) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            MediaQuery find = entityManager.find(MediaQuery.class, mediaQuery.getQueryId());
            if (find != null) entityManager.remove(find);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }

    }

    @Override
    public MediaQuery findQueryByFilePath(String filePath) {
        // typed query throws exception if nothing is found
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        MediaQuery singleResult = null;
        try {
            TypedQuery<MediaQuery> typedQuery = entityManager.createQuery("SELECT q FROM MediaQuery q WHERE q.filePath=:filepath", MediaQuery.class);
            typedQuery.setParameter("filepath", filePath);
            singleResult = typedQuery.getSingleResult();
        } catch (NoResultException e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return singleResult;
    }

    @Override
    public MediaQuery getQueryById(Long id) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        MediaQuery mediaQuery = entityManager.find(MediaQuery.class, id);
        return mediaQuery;
    }


    @Override
    public List<MediaQuery> getAllMediaQueries() {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
//        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//        CriteriaQuery<MediaQuery> cq = cb.createQuery(MediaQuery.class);
//        Root<MediaQuery> rootEntry = cq.from(MediaQuery.class);
//        CriteriaQuery<MediaQuery> all = cq.select(rootEntry);
        List<MediaQuery> resultList = List.of();
        try {
            String all = "SELECT q FROM MediaQuery q";
            TypedQuery<MediaQuery> allQuery = entityManager.createQuery(all, MediaQuery.class);
            resultList = allQuery.getResultList();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return resultList;
    }

    @Override
    public List<MediaQuery> findInFilePathQuery(String phrase) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        List<MediaQuery> mediaQuery = null;
        String query = "%" + phrase + "%";
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<MediaQuery> cr = cb.createQuery(MediaQuery.class);
            Root<MediaQuery> root = cr.from(MediaQuery.class);
            cr.select(root).where(cb.like(root.get("filePath"), query));

            TypedQuery<MediaQuery> mq = entityManager.createQuery(cr);
            mediaQuery = mq.getResultList();
        } catch (NoResultException e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return mediaQuery;
    }


    @Override
    public void addNewLink(MediaLink mediaLInk) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.persist(mediaLInk);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
    }

    @Override
    public void removeLink(MediaLink mediaLInk) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            MediaLink find = entityManager.find(MediaLink.class, mediaLInk.getMediaId());
            entityManager.remove(find);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
    }

    @Override
    public List<MediaLink> getAllMediaLinks() {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        List<MediaLink> resultList = List.of();
        try {
            String all = "SELECT q FROM MediaLink q";
            TypedQuery<MediaLink> allQuery = entityManager.createQuery(all, MediaLink.class);
            resultList = allQuery.getResultList();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return resultList;
    }

    @Override
    public List<MediaLink> findInFilePathLink(String phrase) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        List<MediaLink> mediaQuery = null;
        String query = "%" + phrase + "%";
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<MediaLink> cr = cb.createQuery(MediaLink.class);
            Root<MediaLink> root = cr.from(MediaLink.class);
            cr.select(root).where(cb.like(root.get("targetPath"), query));

            TypedQuery<MediaLink> mq = entityManager.createQuery(cr);
            mediaQuery = mq.getResultList();

        } catch (NoResultException e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return mediaQuery;
    }

    @Override
    public MediaLink findMediaLinkByFilePath(String filePath) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory().createEntityManager();
        MediaLink singleResult = null;
        try {
            TypedQuery<MediaLink> typedQuery = entityManager.createQuery("SELECT q FROM MediaLink q WHERE q.targetPath=:filepath", MediaLink.class);
            typedQuery.setParameter("filepath", filePath);
            singleResult = typedQuery.getSingleResult();
        } catch (NoResultException e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return singleResult;
    }

    public static void main(String[] args) {
        String file = "movies-incoming\\Max.von.Sydow.collection\\Here.Is.Your.Life.1966.720p.BluRay.x264-x0r\\Here.Is.Your.Life.1966.720p.BluRay.x264-x0r.mkv";
        Path testPath = Path.of(file).getParent();

        MediaTrackerDao dao = new MediaTrackerDaoImpl();
        MediaQuery mq = new MediaQuery();
        mq.setFilePath(file);

        dao.addQueryToQueue(mq);

        List<MediaQuery> allMediaQueries = dao.getAllMediaQueries();
        for (MediaQuery mq1 : allMediaQueries) {
            System.out.println(mq1);
        }

//        MediaQuery findQuery = dao.findQueryByFilePath(file);
//        System.out.println(findQuery);
//
//        dao.removeQueryFromQueue(findQuery);
//        allMediaQueries = dao.getAllMediaQueries();
//        for (MediaQuery mq : allMediaQueries) {
//            System.out.println(mq);
//        }


    }

}

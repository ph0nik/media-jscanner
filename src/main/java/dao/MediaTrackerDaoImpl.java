package dao;

import model.MediaIgnored;
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
import java.util.List;


@Component
public class MediaTrackerDaoImpl implements MediaTrackerDao {

    private static final Logger LOG = LoggerFactory.getLogger(MediaTrackerDaoImpl.class);

    private final String persistenceUnit;

    public MediaTrackerDaoImpl() {
        this.persistenceUnit = "jscanner-sqlite";
    }

    public MediaTrackerDaoImpl(String persistenceUnit) {
        this.persistenceUnit = persistenceUnit;
    }

    @Override
    public void addQueryToQueue(MediaQuery query) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
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
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
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
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
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
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        MediaQuery mediaQuery = entityManager.find(MediaQuery.class, id);
        return mediaQuery;
    }


    @Override
    public List<MediaQuery> getAllMediaQueries() {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
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
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
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
    public MediaLink getLinkById(Long id) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        MediaLink mediaLink = entityManager.find(MediaLink.class, id);
        return mediaLink;
    }


    @Override
    public boolean addNewLink(MediaLink mediaLInk) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.persist(mediaLInk);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            LOG.error(e.getMessage());
            return false;
        } finally {
            entityManager.close();
        }
        return true;
    }

    @Override
    public boolean updateLink(MediaLink mediaLink) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        EntityTransaction transaction = null;
        try {
            MediaLink ml = entityManager.find(MediaLink.class, mediaLink.getMediaId());
            transaction = entityManager.getTransaction();
            transaction.begin();
            ml.setLinkPath(mediaLink.getLinkPath());
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            LOG.error(e.getMessage());
            return false;
        } finally {
            entityManager.close();
        }
        return true;
    }


    @Override
    public MediaLink removeLink(Long mediaLinkId) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        EntityTransaction transaction = null;
        MediaLink find = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            find = entityManager.find(MediaLink.class, mediaLinkId);
            entityManager.remove(find);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return find;
    }

    @Override
    public List<MediaLink> getAllMediaLinks() {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
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

    /*
     * Method finds all elements that have given phrase within their target path.
     * */
    @Override
    public List<MediaLink> findInTargetFilePathLink(String phrase) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        List<MediaLink> mediaQuery = null;
        String query = "%" + phrase.replaceAll("\\\\", "%") + "%";
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
    public List<MediaLink> findInLinkFilePathLink(String phrase) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        List<MediaLink> mediaQuery = null;
        String query = "%" + phrase.replaceAll("\\\\", "%") + "%";
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<MediaLink> cr = cb.createQuery(MediaLink.class);
            Root<MediaLink> root = cr.from(MediaLink.class);
            cr.select(root).where(cb.like(root.get("linkPath"), query));

            TypedQuery<MediaLink> mq = entityManager.createQuery(cr);
            mediaQuery = mq.getResultList();

        } catch (NoResultException e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return mediaQuery;
    }

    /*
     * Find media link element by exact target path string.
     * */
    @Override
    public MediaLink findMediaLinkByFilePath(String targetPath) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        MediaLink singleResult = null;
        try {
            TypedQuery<MediaLink> typedQuery = entityManager.createQuery("SELECT q FROM MediaLink q WHERE q.targetPath=:filepath", MediaLink.class);
            typedQuery.setParameter("filepath", targetPath);
            singleResult = typedQuery.getSingleResult();
        } catch (NoResultException e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return singleResult;
    }

    @Override
    public boolean addMediaIgnored(MediaIgnored mediaIgnored) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            entityManager.persist(mediaIgnored);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            LOG.error(e.getMessage());
            return false;
        } finally {
            entityManager.close();
        }
        return true;
    }

    @Override
    public MediaIgnored removeMediaIgnored(Long mediaIgnoredId) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        EntityTransaction transaction = null;
        MediaIgnored find = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            find = entityManager.find(MediaIgnored.class, mediaIgnoredId);
            entityManager.remove(find);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return find;
    }

    @Override
    public MediaIgnored getMediaIgnoredById(Long id) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        return entityManager.find(MediaIgnored.class, id);
    }

    @Override
    public List<MediaIgnored> getAllMediaIgnored() {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        List<MediaIgnored> resultList = List.of();
        try {
            String all = "SELECT q FROM MediaIgnored q";
            TypedQuery<MediaIgnored> allQuery = entityManager.createQuery(all, MediaIgnored.class);
            resultList = allQuery.getResultList();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return resultList;
    }

    @Override
    public MediaIgnored findMediaIgnoredByFilePath(String filePath) {
        EntityManager entityManager = MediaEntityManager.getEntityManagerFactory(persistenceUnit).createEntityManager();
        MediaIgnored singleResult = null;
        try {
            TypedQuery<MediaIgnored> typedQuery = entityManager.createQuery("SELECT q FROM MediaIgnored q WHERE q.targetPath=:filepath", MediaIgnored.class);
            typedQuery.setParameter("filepath", filePath);
            singleResult = typedQuery.getSingleResult();
        } catch (NoResultException e) {
            LOG.error(e.getMessage());
        } finally {
            entityManager.close();
        }
        return singleResult;
    }

}

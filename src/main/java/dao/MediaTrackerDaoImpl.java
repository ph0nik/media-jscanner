package dao;

import model.MediaLink;
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


@Component("hibernate")
public class MediaTrackerDaoImpl implements MediaTrackerDao {

    private static final Logger LOG = LoggerFactory.getLogger(MediaTrackerDaoImpl.class);
    private final String ignorePath = "ignored";

    public MediaTrackerDaoImpl() {
    }

    EntityManager getEntityManager() {
        return MediaEntityManager.getEntityManagerFactory().createEntityManager();
    }

    @Override
    public MediaLink getLinkById(Long id) {
        MediaLink mediaLink = getEntityManager().find(MediaLink.class, id);
        return mediaLink;
    }

    @Override
    public MediaLink addNewLink(MediaLink mediaLInk) {
        EntityManager entityManager = getEntityManager();
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
        return mediaLInk;
    }

    @Override
    public MediaLink updateLink(MediaLink mediaLink) {
        EntityManager entityManager = getEntityManager();
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
        } finally {
            entityManager.close();
        }
        return mediaLink;
    }


    @Override
    public MediaLink removeLink(Long mediaLinkId) {
        EntityManager entityManager = getEntityManager();
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
        EntityManager entityManager = getEntityManager();
        List<MediaLink> resultList = List.of();
        try {
            String all = "SELECT q FROM MediaLink q";
            TypedQuery<MediaLink> allQuery = entityManager.createQuery(all, MediaLink.class);
            resultList = allQuery.getResultList();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
//            entityManager.close();
        }
        return resultList;
    }

    /*
     * Method finds all elements that have given phrase within their target path.
     * */
    @Override
    public List<MediaLink> findInOriginalPathLink(String phrase) {
        EntityManager entityManager = getEntityManager();
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
    public List<MediaLink> findInLinkPathLink(String phrase) {
        EntityManager entityManager = getEntityManager();
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
    public MediaLink getMediaLinkByTargetPath(String targetPath) {
        EntityManager entityManager = getEntityManager();
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

//    @Override
//    public MediaLink addMediaIgnored(MediaLink mediaIgnored) {
//        return addNewLink(mediaIgnored);
//    }
//
//    @Override
//    public MediaLink removeMediaIgnored(Long mediaIgnoredId) {
//        return removeLink(mediaIgnoredId);
//    }
//
//    @Override
//    public MediaLink getMediaIgnoredById(Long id) {
//        return getLinkById(id);
//    }
//
//    @Override
//    public List<MediaLink> getAllMediaIgnored() {
//        return getAllMediaLinks().stream().filter(mi -> mi.getLinkPath().equals(IGNORE_PATH)).collect(Collectors.toList());
////        EntityManager entityManager = getEntityManager();
////        List<MediaLink> resultList = List.of();
////        try {
////            String all = "SELECT q FROM MediaIgnored q";
////            TypedQuery<MediaLink> allQuery = entityManager.createQuery(all, MediaLink.class);
////            resultList = allQuery.getResultList();
////        } catch (Exception e) {
////            LOG.error(e.getMessage());
////        } finally {
////            entityManager.close();
////        }
////        return resultList;
//    }
//
//    @Override
//    public MediaLink findMediaIgnoredByTargetPath(String filePath) {
//        return findMediaLinkByTargetPath(filePath);
////        EntityManager entityManager = getEntityManager();
////        MediaIgnored singleResult = null;
////        try {
////            TypedQuery<MediaIgnored> typedQuery = entityManager.createQuery("SELECT q FROM MediaIgnored q WHERE q.targetPath=:filepath", MediaIgnored.class);
////            typedQuery.setParameter("filepath", filePath);
////            singleResult = typedQuery.getSingleResult();
////        } catch (NoResultException e) {
////            LOG.error(e.getMessage());
////        } finally {
////            entityManager.close();
////        }
////        return singleResult;
//    }

}

package dao;

import model.MediaIgnored;
import model.MediaLink;
import model.MediaQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityExistsException;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;

@Repository("spring")
@Transactional
public class SpringHibernateBootstrapDao implements MediaTrackerDao {

    public static final Logger LOG = LoggerFactory.getLogger(SpringHibernateBootstrapDao.class);

    @Autowired
    private SessionFactory sessionFactory;

    Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public void addQueryToQueue(MediaQuery query) {
        getCurrentSession().save(query);
    }

    @Override
    public void removeQueryFromQueue(MediaQuery query) {
        MediaQuery queryById = getQueryById(query.getQueryId());
        getCurrentSession().delete(queryById);
    }

    @Override
    public MediaQuery findQueryByFilePath(String filePath) {
        String query = "SELECT q FROM MediaQuery q WHERE q.filePath=:filepath";
        Query<MediaQuery> results = getCurrentSession().createQuery(query, MediaQuery.class);
        results.setParameter("filepath", filePath);
        return results.getSingleResult();
    }

    @Override
    public MediaQuery getQueryById(Long id) {
        return getCurrentSession().get(MediaQuery.class, id);
    }

    @Override
    public List<MediaQuery> getAllMediaQueries() {
        String all = "FROM MediaQuery";
        Query<MediaQuery> allQuery = getCurrentSession().createQuery(all, MediaQuery.class);
        return allQuery.getResultList();
    }

    @Override
    public List<MediaQuery> findInFilePathQuery(String phrase) {
        String query = "%" + phrase.replaceAll("\\\\", "%") + "%";
        String select = "SELECT q FROM MediaQuery q where q.filePath like :path";
        Query<MediaQuery> find = getCurrentSession().createQuery(select, MediaQuery.class);
        find.setParameter("path", query);
        return find.getResultList();
    }

    @Override
    public MediaLink getLinkById(Long id) {
        return getCurrentSession().get(MediaLink.class, id);
    }

    @Override
    public void addNewLink(MediaLink mediaLink) {
        getCurrentSession().persist(mediaLink);
    }

    @Override
    public MediaLink updateLink(MediaLink mediaLink) {
        getCurrentSession().merge(mediaLink);
        return mediaLink;
    }

    @Override
    public MediaLink removeLink(Long mediaLinkId) {
        MediaLink mediaLink = getLinkById(mediaLinkId);
        if (mediaLink == null) return null;
        getCurrentSession().delete(mediaLink);
        return mediaLink;
    }

    @Override
    public List<MediaLink> getAllMediaLinks() {
        String all = "FROM MediaLink";
        TypedQuery<MediaLink> allQuery = getCurrentSession().createQuery(all, MediaLink.class);
        List<MediaLink> resultList = allQuery.getResultList();
        return allQuery.getResultList();
    }

    @Override
    public List<MediaLink> findInTargetPathLink(String phrase) {
        return findInPathLink("targetPath", phrase);
    }

    @Override
    public List<MediaLink> findInLinkPathLink(String phrase) {
        return findInPathLink("linkPath", phrase);
    }

    private List<MediaLink> findInPathLink(String pathName, String phrase) {
        String query = "%" + phrase.replaceAll("\\\\", "%") + "%";
        String select = "SELECT q FROM MediaLink q WHERE q." + pathName + " LIKE :path";
        TypedQuery<MediaLink> find = getCurrentSession().createQuery(select, MediaLink.class);
        find.setParameter("path", query);
        return find.getResultList();
    }

    @Override
    public MediaLink findMediaLinkByTargetPath(String filePath) {
        Query<MediaLink> query = getCurrentSession().createQuery("SELECT q FROM MediaLink q WHERE q.filePath=:filepath", MediaLink.class);
        query.setParameter("filepath", filePath);
        return query.getSingleResult();
    }

    @Override
    public boolean addMediaIgnored(MediaIgnored mediaIgnored) {
        try {
            getCurrentSession().persist(mediaIgnored);
            return true;
        } catch (EntityExistsException | TransactionRequiredException ex) {
            return false;
        }
    }

    @Override
    public MediaIgnored removeMediaIgnored(Long id) {
        MediaIgnored mediaIgnoredById = getMediaIgnoredById(id);
        getCurrentSession().delete(mediaIgnoredById);
        return mediaIgnoredById;
    }

    @Override
    public MediaIgnored getMediaIgnoredById(Long id) {
        return getCurrentSession().get(MediaIgnored.class, id);
    }

    @Override
    public List<MediaIgnored> getAllMediaIgnored() {
        String all = "SELECT q FROM MediaIgnored q";
        TypedQuery<MediaIgnored> allMediaIgnored = getCurrentSession().createQuery(all, MediaIgnored.class);
        return allMediaIgnored.getResultList();
    }

    @Override
    public MediaIgnored findMediaIgnoredByTargetPath(String filePath) {
        TypedQuery<MediaIgnored> query = getCurrentSession().createQuery("SELECT q FROM MediaIgnored q WHERE q.targetPath=:filepath", MediaIgnored.class);
        query.setParameter("filepath", filePath);
        return query.getSingleResult();
    }
}

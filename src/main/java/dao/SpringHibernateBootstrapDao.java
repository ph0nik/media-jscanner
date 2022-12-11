package dao;

import model.MediaLink;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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
    public MediaLink getLinkById(Long id) {
        return getCurrentSession().get(MediaLink.class, id);
    }

    @Override
    public MediaLink addNewLink(MediaLink mediaLink) {
        getCurrentSession().persist(mediaLink);
        return mediaLink;
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
        return allQuery.getResultList();
    }

    @Override
    public List<MediaLink> findInOriginalPathLink(String phrase) {
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
    public MediaLink getMediaLinkByTargetPath(String filePath) {
        Query<MediaLink> query = getCurrentSession().createQuery("SELECT q FROM MediaLink q WHERE q.originalPath=:filepath", MediaLink.class);
        query.setParameter("filepath", filePath);
        return query.getSingleResult();
    }

//    @Override
//    public List<MediaLink> getAllIgnoredMedia() {
//        return null;
//    }
//
//    @Override
//    public List<MediaLink> searchInIgnoredMedia(String phrase) {
//        return null;
//    }
}

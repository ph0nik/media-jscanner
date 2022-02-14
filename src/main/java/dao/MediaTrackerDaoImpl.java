package dao;

import model.MediaLink;
import model.MediaQuery;

import java.util.List;

public class MediaTrackerDaoImpl implements MediaTrackerDao {

    @Override
    public void addQueryToQueue(MediaQuery query) {

    }

    @Override
    public void removeQueryFromQueue(MediaQuery query) {

    }

    @Override
    public MediaQuery findQueryByName(String path) {
        return null;
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
    public MediaLink findMediaLinkByName(String name) {
        return null;
    }
}

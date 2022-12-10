package dao;

import model.MediaLink;

import java.util.List;

public interface DataBaseService {

    public List<MediaLink> importLinks(String filePath);

    public void exportLinks(String filePath);
}

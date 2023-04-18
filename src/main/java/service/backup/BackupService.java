package service.backup;

import model.MediaLink;
import service.backup.model.CsvBean;

import java.nio.file.Path;
import java.util.List;

public interface BackupService {

    /*
    * Exports provided list of MediaLink objects as XML file.
    * Created filename is based of current date and time.
    * Returns filename.
    * */
    public String exportRecords(List<MediaLink> mediaLinkList); //TODO

    /*
    * Imports list of MediaLink objects from a file.
    * */
    public List<MediaLink> importRecords(String fileName);

    /*
    * Checks if data folder is present.
    * */
    public boolean checkAppDataFolder();

    /*
    * General builder for list of beans. It accepts filename and class type.
    * */
    public List<? extends CsvBean> beanBuilder(Path path, Class<? extends CsvBean> clazz);

    /*
    * Writes a file from a list of beans.
    * */
    public void writeCsvFromBean(Path path, List<CsvBean> beans);
}

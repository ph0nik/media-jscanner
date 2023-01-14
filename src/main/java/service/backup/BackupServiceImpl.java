package service.backup;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import model.MediaLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.backup.model.CsvBean;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BackupServiceImpl implements BackupService {

    private static final Logger LOG = LoggerFactory.getLogger(BackupServiceImpl.class);
    private String dataFolder;

    public BackupServiceImpl(String dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public String exportRecords(List<MediaLink> mediaLinkList) {
        DateTimeFormatter timeStamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String backupFileName = "backup-" + timeStamp.format(LocalDateTime.now(ZoneId.systemDefault())) + ".csv";
        if (checkAppDataFolder()) {
            Path fullPath = Path.of(dataFolder).resolve(backupFileName);
            List<CsvBean> beans = new ArrayList<>(mediaLinkList);
            writeCsvFromBean(fullPath, beans);
        } else {
            LOG.error("[ backup_service ] Error: no data folder found");
        }
        return backupFileName;
    }

    @Override
    public List<MediaLink> importRecords(String fileName) {
        Path fullPath = Path.of(dataFolder).resolve(fileName);
        List<CsvBean> csvBeans = beanBuilder(fullPath, MediaLink.class);
        return csvBeans.stream().map(x -> (MediaLink) x).collect(Collectors.toList());
    }

    @Override
    public boolean checkAppDataFolder() {
        return Files.exists(Path.of(dataFolder));
    }

    @Override
    public List<CsvBean> beanBuilder(Path path, Class<? extends CsvBean> clazz) {
        try (Reader reader = Files.newBufferedReader(path)) {
            CsvToBean<CsvBean> csvToBean = new CsvToBeanBuilder<CsvBean>(reader)
                    .withQuoteChar('\'')
                    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                    .withType(clazz)
                    .build();
            return csvToBean.parse();
        } catch (IOException e) {
            LOG.error("[ bean_reader ] error: {}",e.getMessage());
        }
        return new ArrayList<>();
    }

    @Override
    public void writeCsvFromBean(Path path, List<CsvBean> beans) {
        try (Writer writer = new FileWriter(path.toFile())) {
            LOG.info("[ bean_writer ] saving...");
            StatefulBeanToCsv<CsvBean> beanToCsv = new StatefulBeanToCsvBuilder<CsvBean>(writer)
                    .withQuotechar('\'')
                    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                    .build();
            beanToCsv.write(beans);
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            LOG.error("[ bean_writer ] {}", e.getMessage());
        }

    }

    public static void main(String[] args) {
        MediaLink ml = new MediaLink();
        ml.setImdbId("tt123435");
        ml.setLinkPath("link path");
        ml.setOriginalPath("original path");
        ml.setMediaId(1L);
        ml.setTheMovieDbId(12345);
        ml.setOriginalPresent(true);
        List<MediaLink> mediaLinkList = new ArrayList<>();
        mediaLinkList.add(ml);

        BackupService bs = new BackupServiceImpl("data");
        String s = bs.exportRecords(mediaLinkList);

        List<MediaLink> mediaLinkList1 = bs.importRecords(s);
        mediaLinkList1.forEach(System.out::println);
    }
}

package model;


import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "media_query")
public class MediaQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long queryId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    public MediaQuery() {}

    public MediaQuery(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public String getQuery() {
        String substring = fileName.substring(0, fileName.lastIndexOf("."));
        return substring.replace(".", " ");
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaQuery that = (MediaQuery) o;
        return fileName.equals(that.fileName) && filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, filePath);
    }

    @Override
    public String toString() {
        return "MediaQuery{" +
                "queryId=" + queryId +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}

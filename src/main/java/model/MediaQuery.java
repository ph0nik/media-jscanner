package model;


import javax.persistence.*;
import java.nio.file.Path;
import java.util.Objects;

@Entity
@Table(name = "media_query")
public class MediaQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long queryId;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "parent_path")
    private String parentPath;

    public MediaQuery() {}

    public MediaQuery(String filePath, String parent) {
        this.filePath = filePath;
        this.parentPath = parent;
    }

    public long getQueryId() {
        return queryId;
    }

    public void setQueryId(long queryId) {
        this.queryId = queryId;
    }

    public String getQuery() {
        Path path = Path.of(filePath);
        String fileName = path.getName(path.getNameCount() - 1).toString();
        String sub = fileName.substring(0, fileName.lastIndexOf("."));
        return sub.replace(".", " ");
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaQuery that = (MediaQuery) o;
        return filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath);
    }

    @Override
    public String toString() {
        return "MediaQuery{" +
                "queryId=" + queryId +
                ", filePath='" + filePath + '\'' +
                ", parentPath='" + parentPath + '\'' +
                '}';
    }
}

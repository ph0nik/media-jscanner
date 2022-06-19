package model;


import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "media_query")
public class MediaQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long queryId;

    @Transient
    private UUID queryUuid;

    @Column(name = "file_path", unique = true)
    @NotNull
    private String filePath;

    public MediaQuery() {}

    public MediaQuery(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        Path of = Path.of(filePath);
        return of.getName(of.getNameCount() - 1).toString();
    }

    public String getParentPath() {
        Path of = Path.of(filePath);
        return of.getParent().toString();
    }

    public UUID getQueryUuid() {
        return queryUuid;
    }

    public void setQueryUuid(UUID queryUuid) {
        this.queryUuid = queryUuid;
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
                ", queryUuid=" + queryUuid +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}

package model;


import javax.persistence.*;

@Entity
@Table(name = "media_link")
public class MediaLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long mediaId;

    @Column(name = "link_path")
    private String linkPath;

    @Column(name = "target_path")
    private String targetPath;

    @Column(name = "parent_path")
    private String parentPath;

    @Column(name = "the_movie_db_id")
    private int theMovieDbId;

    public long getMediaId() {
        return mediaId;
    }

    public void setMediaId(long mediaId) {
        this.mediaId = mediaId;
    }

    public String getLinkPath() {
        return linkPath;
    }

    public void setLinkPath(String linkPath) {
        this.linkPath = linkPath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public int getTheMovieDbId() {
        return theMovieDbId;
    }

    public void setTheMovieDbId(int theMovieDbId) {
        this.theMovieDbId = theMovieDbId;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    @Override
    public String toString() {
        return "MediaLink{" +
                "mediaId=" + mediaId +
                ", linkPath='" + linkPath + '\'' +
                ", targetPath='" + targetPath + '\'' +
                ", parentPath='" + parentPath + '\'' +
                ", theMovieDbId=" + theMovieDbId +
                '}';
    }
}

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

    @Column(name = "the_movie_db_id")
    private int theMovieDbId;

    private String sourceParentPath;

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

    public String getSourceParentPath() {
        return sourceParentPath;
    }

    public void setSourceParentPath(String sourceParentPath) {
        this.sourceParentPath = sourceParentPath;
    }

    @Override
    public String toString() {
        return "MediaLink{" +
                "mediaId=" + mediaId +
                ", sourcePath='" + linkPath + '\'' +
                ", destPath='" + targetPath + '\'' +
                ", theMovieDbId='" + theMovieDbId + '\'' +
                ", sourceParentPath='" + sourceParentPath + '\'' +
                '}';
    }
}

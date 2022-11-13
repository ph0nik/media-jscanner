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

    @Column(name = "target_path", unique = true)
    private String originalPath;

    @Column(name = "the_movie_db_id")
    private int theMovieDbId;

    @Column(name = "imdb_id")
    private String imdbId;

    @Column(name = "original_present")
    private boolean originalPresent = true;

    public boolean isOriginalPresent() {
        return originalPresent;
    }

    public void setOriginalPresent(boolean originalPresent) {
        this.originalPresent = originalPresent;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

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

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public int getTheMovieDbId() {
        return theMovieDbId;
    }

    public void setTheMovieDbId(int theMovieDbId) {
        this.theMovieDbId = theMovieDbId;
    }

    @Override
    public String toString() {
        return "MediaLink{" +
                "mediaId=" + mediaId +
                ", linkPath='" + linkPath + '\'' +
                ", originalPath='" + originalPath + '\'' +
                ", theMovieDbId=" + theMovieDbId +
                ", imdbId='" + imdbId + '\'' +
                ", originalExists=" + originalPresent +
                '}';
    }
}

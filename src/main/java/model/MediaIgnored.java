//package model;
//
//import javax.persistence.*;
//
//@Entity
//@Table(name = "media_ignored")
//public class MediaIgnored {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id")
//    private long mediaId;
//
//    @Column(name = "target_path", unique = true)
//    private String targetPath;
//
//    public long getMediaId() {
//        return mediaId;
//    }
//
//    public void setMediaId(long mediaId) {
//        this.mediaId = mediaId;
//    }
//
//    public String getTargetPath() {
//        return targetPath;
//    }
//
//    public void setTargetPath(String targetPath) {
//        this.targetPath = targetPath;
//    }
//
//    @Override
//    public String toString() {
//        return "MediaIgnored{" +
//                "mediaId=" + mediaId +
//                ", targetPath='" + targetPath + '\'' +
//                '}';
//    }
//}

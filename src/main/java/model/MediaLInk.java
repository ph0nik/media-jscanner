package model;

import javax.persistence.*;

@Entity
@Table(name = "media_link")
public class MediaLInk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long mediaId;

    @Column(name = "source_path")
    private String sourcePath;

    @Column(name = "destination_path")
    private String destPath;


    private String sourceParentPath;


}

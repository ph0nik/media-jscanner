package dao;

import model.MediaLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaLinkRepository extends JpaRepository<MediaLink, Long> {

    List<MediaLink> findByOriginalPathContaining(String phrase);

    List<MediaLink> findByLinkPathContaining(String phrase);

    MediaLink findByOriginalPath(String path);
}

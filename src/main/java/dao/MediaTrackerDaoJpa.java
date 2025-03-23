package dao;

import jakarta.transaction.Transactional;
import model.MediaLink;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

//@Repository("jpa")
@Transactional
@Service("jpa")
public class MediaTrackerDaoJpa implements MediaTrackerDao{

    private final MediaLinkRepository mediaLinkRepository;

    public MediaTrackerDaoJpa(MediaLinkRepository mediaLinkRepository) {
        this.mediaLinkRepository = mediaLinkRepository;
    }

    @Override
    public MediaLink getLinkById(Long id) {
        Optional<MediaLink> byId = mediaLinkRepository.findById(id);
        return byId.orElse(null);
    }

    @Override
    public MediaLink addNewLink(MediaLink mediaLInk) {
        return mediaLinkRepository.save(mediaLInk);
    }

    @Override
    public MediaLink updateLink(MediaLink mediaLink) {
        return null;
    }

    @Override
    public void removeLink(Long mediaLinkId) {
        mediaLinkRepository.deleteById(mediaLinkId);
    }

    @Override
    public List<MediaLink> getAllMediaLinks() {
        return mediaLinkRepository.findAll();
    }

    @Override
    public List<MediaLink> findInOriginalPathLink(String phrase) {
        return mediaLinkRepository.findByOriginalPathContaining(phrase);
    }

    @Override
    public List<MediaLink> findInLinkPathLink(String phrase) {
        return mediaLinkRepository.findByLinkPathContaining(phrase);
    }

    @Override
    public MediaLink getMediaLinkByTargetPath(String filePath) {
        return mediaLinkRepository.findByOriginalPath(filePath);
    }
}

package service;

import model.MediaLink;
import model.duplicates.DuplicateMediaLinkDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class DuplicateResolverServiceImpl implements DuplicateResolverService {

    private static final Logger LOG = LoggerFactory.getLogger(DuplicateResolverServiceImpl.class);

    @Override
    public DuplicateMediaLinkDto getDuplicateDto(
            MediaLink existingMediaLink,
            MediaLink newMediaLink
    ) {
        try {
            long existingFileSize = (long) Files.getAttribute(Path.of(existingMediaLink.getOriginalPath()), "size");
            long newFileSize = (long) Files.getAttribute(Path.of(newMediaLink.getOriginalPath()), "size");
            return new DuplicateMediaLinkDto(
                    getFileNameFromPath(newMediaLink.getLinkPath()),
                    getParentFolder(newMediaLink.getLinkPath()),
                    newMediaLink.getOriginalPath(),
                    existingMediaLink.getOriginalPath(),
                    existingFileSize,
                    newFileSize
            );
        } catch (IOException e) {
            LOG.error("[ duplicate_resolver ] Error getting file size: {}", e.getMessage());
        }
        return null;
    }

    /*
     * Checks if link with the same path already exist
     * */
    @Override
    public MediaLink linkRecordExist(List<MediaLink> mediaLinks, MediaLink mediaLink) {
        return mediaLinks
                .stream()
                .filter(ml ->
                        ml.getLinkPath().equals(mediaLink.getLinkPath()))
                .findFirst()
                .orElse(null);
//                .anyMatch(ml ->
//                        ml.getLinkPath().equals(mediaLink.getLinkPath()));
    }

    private String getFileNameFromPath(String path) {
        return Path.of(path)
                .getFileName()
                .toString();
    }

    private String getParentFolder(String path) {
        return Path.of(path)
                .getParent()
                .toString();
    }

}

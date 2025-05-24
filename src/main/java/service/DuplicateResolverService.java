package service;

import model.MediaLink;
import model.duplicates.DuplicateMediaLinkDto;

import java.util.List;

public interface DuplicateResolverService {
    DuplicateMediaLinkDto getDuplicateDto(
            MediaLink existingMediaLink,
            MediaLink newMediaLink
    );

    /*
     * Checks if link with the same path already exist
     * */
    MediaLink linkRecordExist(List<MediaLink> mediaLinks, MediaLink mediaLink);
}

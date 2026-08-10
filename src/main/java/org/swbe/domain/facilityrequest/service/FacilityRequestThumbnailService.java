package org.swbe.domain.facilityrequest.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.file.service.FilePublicUrlResolver;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityRequestThumbnailService {

  private final FacilityRequestAttachmentRepository attachmentRepository;
  private final FilePublicUrlResolver filePublicUrlResolver;

  public Map<Long, String> resolveAll(List<Long> facilityRequestIds) {
    if (facilityRequestIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> thumbnailUrls = new LinkedHashMap<>();
    for (FacilityRequestAttachment attachment : attachmentRepository
        .findPublicImagesByFacilityRequestIds(facilityRequestIds)) {
      Long facilityRequestId = attachment.getFacilityRequest().getId();
      if (!thumbnailUrls.containsKey(facilityRequestId)) {
        thumbnailUrls.put(
            facilityRequestId,
            filePublicUrlResolver.resolve(attachment.getFile())
        );
      }
    }
    return Map.copyOf(thumbnailUrls);
  }
}

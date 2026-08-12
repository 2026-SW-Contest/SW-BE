package org.swbe.domain.lostitem.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.service.FilePublicUrlResolver;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;
import org.swbe.domain.lostitem.repository.StoredItemAttachmentRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoredItemThumbnailService {

  private final StoredItemAttachmentRepository attachmentRepository;
  private final FilePublicUrlResolver filePublicUrlResolver;

  public Map<Long, String> resolveAll(List<Long> storedItemIds) {
    if (storedItemIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, String> thumbnailUrls = new LinkedHashMap<>();
    for (StoredItemAttachment attachment : attachmentRepository
        .findPublicImagesByStoredItemIds(storedItemIds)) {
      Long storedItemId = attachment.getStoredItem().getId();
      if (!thumbnailUrls.containsKey(storedItemId)) {
        thumbnailUrls.put(
            storedItemId,
            filePublicUrlResolver.resolve(attachment.getFile())
        );
      }
    }
    return Map.copyOf(thumbnailUrls);
  }
}

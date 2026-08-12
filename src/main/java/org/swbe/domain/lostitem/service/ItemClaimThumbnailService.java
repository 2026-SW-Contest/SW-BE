package org.swbe.domain.lostitem.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.file.service.PrivateFileUrlResolver;
import org.swbe.domain.lostitem.entity.ItemClaimAttachment;
import org.swbe.domain.lostitem.repository.ItemClaimAttachmentRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemClaimThumbnailService {

  private final ItemClaimAttachmentRepository attachmentRepository;
  private final PrivateFileUrlResolver privateFileUrlResolver;

  public Map<Long, ItemClaimAttachmentSummary> resolveAll(
      List<Long> itemClaimIds
  ) {
    if (itemClaimIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, MutableSummary> summaries = new LinkedHashMap<>();
    for (ItemClaimAttachment attachment : attachmentRepository
        .findPublicImagesByItemClaimIds(itemClaimIds)) {
      Long itemClaimId = attachment.getItemClaim().getId();
      MutableSummary summary = summaries.computeIfAbsent(
          itemClaimId,
          ignored -> new MutableSummary()
      );
      summary.attachmentCount++;
      if (summary.thumbnailUrl == null) {
        summary.thumbnailUrl = privateFileUrlResolver.resolve(
            attachment.getFile()
        );
      }
    }

    Map<Long, ItemClaimAttachmentSummary> result = new LinkedHashMap<>();
    summaries.forEach((itemClaimId, summary) -> result.put(
        itemClaimId,
        new ItemClaimAttachmentSummary(
            summary.thumbnailUrl,
            summary.attachmentCount
        )
    ));
    return Map.copyOf(result);
  }

  private static class MutableSummary {
    private String thumbnailUrl;
    private int attachmentCount;
  }
}

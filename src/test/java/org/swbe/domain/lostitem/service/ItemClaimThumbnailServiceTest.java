package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.PrivateFileUrlResolver;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimAttachment;
import org.swbe.domain.lostitem.repository.ItemClaimAttachmentRepository;

class ItemClaimThumbnailServiceTest {

  @Test
  void resolvesFirstImageAndCountsAllAttachmentsInOneResult() {
    ItemClaimAttachmentRepository repository = mock(
        ItemClaimAttachmentRepository.class
    );
    PrivateFileUrlResolver resolver = mock(PrivateFileUrlResolver.class);
    ItemClaim claim = mock(ItemClaim.class);
    ItemClaimAttachment first = attachment(claim);
    ItemClaimAttachment second = attachment(claim);
    when(claim.getId()).thenReturn(31L);
    when(repository.findPublicImagesByItemClaimIds(List.of(31L)))
        .thenReturn(List.of(first, second));
    when(resolver.resolve(first.getFile()))
        .thenReturn("https://cdn/first.jpg");
    ItemClaimThumbnailService service = new ItemClaimThumbnailService(
        repository,
        resolver
    );

    var summaries = service.resolveAll(List.of(31L));

    assertThat(summaries.get(31L).thumbnailUrl())
        .isEqualTo("https://cdn/first.jpg");
    assertThat(summaries.get(31L).attachmentCount()).isEqualTo(2);
  }

  private ItemClaimAttachment attachment(ItemClaim claim) {
    ItemClaimAttachment attachment = mock(ItemClaimAttachment.class);
    when(attachment.getItemClaim()).thenReturn(claim);
    when(attachment.getFile()).thenReturn(mock(FileResource.class));
    return attachment;
  }
}

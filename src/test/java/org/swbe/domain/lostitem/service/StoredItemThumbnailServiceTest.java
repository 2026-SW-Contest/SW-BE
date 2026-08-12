package org.swbe.domain.lostitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.FilePublicUrlResolver;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemAttachment;
import org.swbe.domain.lostitem.repository.StoredItemAttachmentRepository;

class StoredItemThumbnailServiceTest {

  private StoredItemAttachmentRepository attachmentRepository;
  private FilePublicUrlResolver filePublicUrlResolver;
  private StoredItemThumbnailService service;

  @BeforeEach
  void setUp() {
    attachmentRepository = mock(StoredItemAttachmentRepository.class);
    filePublicUrlResolver = mock(FilePublicUrlResolver.class);
    service = new StoredItemThumbnailService(
        attachmentRepository,
        filePublicUrlResolver
    );
  }

  @Test
  void usesFirstOrderedAttachmentAsThumbnail() {
    StoredItem item = mock(StoredItem.class);
    when(item.getId()).thenReturn(30L);
    FileResource primaryFile = mock(FileResource.class);
    FileResource otherFile = mock(FileResource.class);
    StoredItemAttachment primary = attachment(item, primaryFile);
    StoredItemAttachment other = attachment(item, otherFile);
    when(attachmentRepository.findPublicImagesByStoredItemIds(
        List.of(30L)
    )).thenReturn(List.of(primary, other));
    when(filePublicUrlResolver.resolve(primaryFile))
        .thenReturn("https://cdn.example.com/primary.jpg");

    Map<Long, String> result = service.resolveAll(List.of(30L));

    assertThat(result).containsEntry(
        30L,
        "https://cdn.example.com/primary.jpg"
    );
  }

  @Test
  void emptyIdsDoNotQueryAttachments() {
    assertThat(service.resolveAll(List.of())).isEmpty();

    verifyNoInteractions(attachmentRepository, filePublicUrlResolver);
  }

  private StoredItemAttachment attachment(
      StoredItem item,
      FileResource file
  ) {
    StoredItemAttachment attachment = mock(StoredItemAttachment.class);
    when(attachment.getStoredItem()).thenReturn(item);
    when(attachment.getFile()).thenReturn(file);
    return attachment;
  }
}

package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.FilePublicUrlResolver;

class FacilityRequestThumbnailServiceTest {

  private FacilityRequestAttachmentRepository attachmentRepository;
  private FilePublicUrlResolver filePublicUrlResolver;
  private FacilityRequestThumbnailService service;

  @BeforeEach
  void setUp() {
    attachmentRepository = mock(FacilityRequestAttachmentRepository.class);
    filePublicUrlResolver = mock(FilePublicUrlResolver.class);
    service = new FacilityRequestThumbnailService(
        attachmentRepository,
        filePublicUrlResolver
    );
  }

  @Test
  void usesFirstPublicAttachmentAsThumbnail() {
    FacilityRequest request = mock(FacilityRequest.class);
    when(request.getId()).thenReturn(25L);
    FileResource firstFile = mock(FileResource.class);
    FileResource secondFile = mock(FileResource.class);
    FacilityRequestAttachment first = attachment(request, firstFile);
    FacilityRequestAttachment second = attachment(request, secondFile);
    when(attachmentRepository.findPublicImagesByFacilityRequestIds(
        List.of(25L)
    )).thenReturn(List.of(first, second));
    when(filePublicUrlResolver.resolve(firstFile))
        .thenReturn("https://cdn.example.com/first.jpg");

    Map<Long, String> result = service.resolveAll(List.of(25L));

    assertThat(result).containsEntry(
        25L,
        "https://cdn.example.com/first.jpg"
    );
  }

  @Test
  void emptyIdsDoNotQueryAttachments() {
    assertThat(service.resolveAll(List.of())).isEmpty();

    verifyNoInteractions(attachmentRepository, filePublicUrlResolver);
  }

  private FacilityRequestAttachment attachment(
      FacilityRequest request,
      FileResource file
  ) {
    FacilityRequestAttachment attachment = mock(
        FacilityRequestAttachment.class
    );
    when(attachment.getFacilityRequest()).thenReturn(request);
    when(attachment.getFile()).thenReturn(file);
    return attachment;
  }
}

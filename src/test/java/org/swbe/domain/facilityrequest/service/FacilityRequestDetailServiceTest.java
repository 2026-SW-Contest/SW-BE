package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
class FacilityRequestDetailServiceTest {

  @Mock
  private FacilityRequestRepository facilityRequestRepository;

  @Mock
  private FacilityRequestAttachmentRepository attachmentRepository;

  @InjectMocks
  private FacilityRequestDetailService facilityRequestDetailService;

  @Test
  void anonymousUserCanViewRequestWithAttachments() {
    FacilityRequest request = request(10L, "IN_PROGRESS");
    FileResource file = mock(FileResource.class);
    when(file.getId()).thenReturn(15L);
    when(file.getOriginalFilename()).thenReturn("broken-light.jpg");
    FacilityRequestAttachment attachment = mock(FacilityRequestAttachment.class);
    when(attachment.getFile()).thenReturn(file);
    when(facilityRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));
    when(attachmentRepository
        .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(10L))
        .thenReturn(List.of(attachment));

    FacilityRequestDetailResponse response =
        facilityRequestDetailService.getFacilityRequest(
        10L,
        null
    );

    assertThat(response.data().facilityRequestId()).isEqualTo(10L);
    assertThat(response.data().requestStatusName()).isNotBlank();
    assertThat(response.data().attachments()).hasSize(1);
    assertThat(response.data().attachments().getFirst().fileId())
        .isEqualTo(15L);
    assertThat(response.data().attachments().getFirst().fileUrl()).isNull();
    assertThat(response.data().editable()).isFalse();
    assertThat(response.data().deletable()).isFalse();
  }

  @Test
  void ownerCanEditAndDeleteReceivedRequest() {
    FacilityRequest request = request(10L, "RECEIVED");
    when(request.isRequestedBy(7L)).thenReturn(true);
    when(request.isEditable()).thenReturn(true);
    when(facilityRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));
    when(attachmentRepository
        .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(10L))
        .thenReturn(List.of());

    FacilityRequestDetailResponse response =
        facilityRequestDetailService.getFacilityRequest(
        10L,
        7L
    );

    assertThat(response.data().editable()).isTrue();
    assertThat(response.data().deletable()).isTrue();
  }

  @Test
  void nonOwnerCanViewRequestButCannotEditOrDelete() {
    FacilityRequest request = request(10L, "CHECKING");
    when(facilityRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));
    when(attachmentRepository
        .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(10L))
        .thenReturn(List.of());

    FacilityRequestDetailResponse response =
        facilityRequestDetailService.getFacilityRequest(
        10L,
        9L
    );

    assertThat(response.data().facilityRequestId()).isEqualTo(10L);
    assertThat(response.data().editable()).isFalse();
    assertThat(response.data().deletable()).isFalse();
  }

  @Test
  void missingRequestReturnsNotFound() {
    when(facilityRequestRepository.findDetailById(99L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> facilityRequestDetailService.getFacilityRequest(99L, null)
    )
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode()
        ).isEqualTo(FacilityRequestErrorCode.NOT_FOUND));
    verifyNoInteractions(attachmentRepository);
  }

  private FacilityRequest request(
      Long id,
      String status
  ) {
    FacilityCategory category = mock(FacilityCategory.class);
    when(category.getId()).thenReturn(1L);
    when(category.getName()).thenReturn("Electricity/Lighting");
    Location location = mock(Location.class);
    when(location.getId()).thenReturn(2L);
    when(location.getName()).thenReturn("Student Center");
    FacilityRequest request = mock(FacilityRequest.class);
    when(request.getId()).thenReturn(id);
    when(request.getTitle()).thenReturn("Flickering hallway light");
    when(request.getDescription()).thenReturn(
        "The hallway light keeps flickering."
    );
    when(request.getFacilityCategory()).thenReturn(category);
    when(request.getLocation()).thenReturn(location);
    when(request.getRequestStatus()).thenReturn(status);
    when(request.getCreatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 1, 16, 0)
    );
    when(request.getUpdatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 1, 16, 10)
    );
    return request;
  }
}

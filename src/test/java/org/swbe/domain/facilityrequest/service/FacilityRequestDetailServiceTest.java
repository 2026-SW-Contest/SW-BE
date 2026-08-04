package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
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
import org.swbe.domain.facilityrequest.repository.RequestAssignmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
class FacilityRequestDetailServiceTest {

  @Mock
  private FacilityRequestRepository facilityRequestRepository;

  @Mock
  private RequestAssignmentRepository requestAssignmentRepository;

  @Mock
  private FacilityRequestAttachmentRepository attachmentRepository;

  @InjectMocks
  private FacilityRequestDetailService facilityRequestDetailService;

  @Test
  void anonymousUserCanViewPublicRequestWithAttachments() {
    FacilityRequest request = request(10L, "PUBLIC", 7L, "IN_PROGRESS");
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

    var response = facilityRequestDetailService.getFacilityRequest(
        10L,
        null,
        false
    );

    assertThat(response.data().facilityRequestId()).isEqualTo(10L);
    assertThat(response.data().requestStatusName()).isNotBlank();
    assertThat(response.data().attachments()).hasSize(1);
    assertThat(response.data().attachments().getFirst().fileId())
        .isEqualTo(15L);
    assertThat(response.data().attachments().getFirst().fileUrl()).isNull();
    assertThat(response.data().editable()).isFalse();
    assertThat(response.data().deletable()).isFalse();
    verifyNoInteractions(requestAssignmentRepository);
  }

  @Test
  void ownerCanEditAndDeleteReceivedPrivateRequest() {
    FacilityRequest request = request(10L, "PRIVATE", 7L, "RECEIVED");
    when(facilityRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));
    when(attachmentRepository
        .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(10L))
        .thenReturn(List.of());

    var response = facilityRequestDetailService.getFacilityRequest(
        10L,
        7L,
        false
    );

    assertThat(response.data().editable()).isTrue();
    assertThat(response.data().deletable()).isTrue();
    verifyNoInteractions(requestAssignmentRepository);
  }

  @Test
  void currentlyAssignedUserCanViewPrivateRequest() {
    FacilityRequest request = request(10L, "PRIVATE", 7L, "CHECKING");
    when(facilityRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));
    when(requestAssignmentRepository
        .existsByFacilityRequest_IdAndAssignedUser_IdAndEndedAtIsNull(10L, 9L))
        .thenReturn(true);
    when(attachmentRepository
        .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(10L))
        .thenReturn(List.of());

    var response = facilityRequestDetailService.getFacilityRequest(
        10L,
        9L,
        false
    );

    assertThat(response.data().facilityRequestId()).isEqualTo(10L);
    assertThat(response.data().editable()).isFalse();
  }

  @Test
  void unauthorizedPrivateRequestIsHiddenAsNotFound() {
    FacilityRequest request = mock(FacilityRequest.class);
    when(request.getVisibility()).thenReturn("PRIVATE");
    when(facilityRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));

    assertThatThrownBy(
        () -> facilityRequestDetailService.getFacilityRequest(10L, null, false)
    )
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode()
        ).isEqualTo(FacilityRequestErrorCode.NOT_FOUND));
    verifyNoInteractions(
        requestAssignmentRepository,
        attachmentRepository
    );
  }

  @Test
  void missingRequestReturnsNotFound() {
    when(facilityRequestRepository.findDetailById(99L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> facilityRequestDetailService.getFacilityRequest(99L, null, false)
    )
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode()
        ).isEqualTo(FacilityRequestErrorCode.NOT_FOUND));
    verifyNoInteractions(
        requestAssignmentRepository,
        attachmentRepository
    );
  }

  private FacilityRequest request(
      Long id,
      String visibility,
      Long requesterId,
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
    lenient().when(request.isRequestedBy(requesterId)).thenReturn(true);
    when(request.getVisibility()).thenReturn(visibility);
    when(request.getReceiptNumber()).thenReturn("SR-20260801-0001");
    when(request.getTitle()).thenReturn("Flickering hallway light");
    when(request.getDescription()).thenReturn(
        "The hallway light keeps flickering."
    );
    when(request.getEquipmentName()).thenReturn("LED light");
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

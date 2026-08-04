package org.swbe.domain.servicerequest.service;

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
import org.swbe.domain.servicerequest.entity.RequestCategory;
import org.swbe.domain.servicerequest.entity.ServiceRequest;
import org.swbe.domain.servicerequest.entity.ServiceRequestAttachment;
import org.swbe.domain.servicerequest.exception.ServiceRequestErrorCode;
import org.swbe.domain.servicerequest.repository.RequestAssignmentRepository;
import org.swbe.domain.servicerequest.repository.ServiceRequestAttachmentRepository;
import org.swbe.domain.servicerequest.repository.ServiceRequestRepository;
import org.swbe.global.error.BusinessException;

@ExtendWith(MockitoExtension.class)
class ServiceRequestDetailServiceTest {

  @Mock
  private ServiceRequestRepository serviceRequestRepository;

  @Mock
  private RequestAssignmentRepository requestAssignmentRepository;

  @Mock
  private ServiceRequestAttachmentRepository attachmentRepository;

  @InjectMocks
  private ServiceRequestDetailService serviceRequestDetailService;

  @Test
  void anonymousUserCanViewPublicRequestWithAttachments() {
    ServiceRequest request = request(10L, "PUBLIC", 7L, "IN_PROGRESS");
    FileResource file = mock(FileResource.class);
    when(file.getId()).thenReturn(15L);
    when(file.getOriginalFilename()).thenReturn("broken-light.jpg");
    ServiceRequestAttachment attachment = mock(ServiceRequestAttachment.class);
    when(attachment.getFile()).thenReturn(file);
    when(serviceRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));
    when(attachmentRepository
        .findAllByServiceRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(10L))
        .thenReturn(List.of(attachment));

    var response = serviceRequestDetailService.getServiceRequest(
        10L,
        null,
        false
    );

    assertThat(response.data().serviceRequestId()).isEqualTo(10L);
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
    ServiceRequest request = request(10L, "PRIVATE", 7L, "RECEIVED");
    when(serviceRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));
    when(attachmentRepository
        .findAllByServiceRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(10L))
        .thenReturn(List.of());

    var response = serviceRequestDetailService.getServiceRequest(
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
    ServiceRequest request = request(10L, "PRIVATE", 7L, "CHECKING");
    when(serviceRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));
    when(requestAssignmentRepository
        .existsByServiceRequest_IdAndAssignedUser_IdAndEndedAtIsNull(10L, 9L))
        .thenReturn(true);
    when(attachmentRepository
        .findAllByServiceRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(10L))
        .thenReturn(List.of());

    var response = serviceRequestDetailService.getServiceRequest(
        10L,
        9L,
        false
    );

    assertThat(response.data().serviceRequestId()).isEqualTo(10L);
    assertThat(response.data().editable()).isFalse();
  }

  @Test
  void unauthorizedPrivateRequestIsHiddenAsNotFound() {
    ServiceRequest request = mock(ServiceRequest.class);
    when(request.getVisibility()).thenReturn("PRIVATE");
    when(serviceRequestRepository.findDetailById(10L))
        .thenReturn(Optional.of(request));

    assertThatThrownBy(
        () -> serviceRequestDetailService.getServiceRequest(10L, null, false)
    )
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode()
        ).isEqualTo(ServiceRequestErrorCode.NOT_FOUND));
    verifyNoInteractions(
        requestAssignmentRepository,
        attachmentRepository
    );
  }

  @Test
  void missingRequestReturnsNotFound() {
    when(serviceRequestRepository.findDetailById(99L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> serviceRequestDetailService.getServiceRequest(99L, null, false)
    )
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode()
        ).isEqualTo(ServiceRequestErrorCode.NOT_FOUND));
    verifyNoInteractions(
        requestAssignmentRepository,
        attachmentRepository
    );
  }

  private ServiceRequest request(
      Long id,
      String visibility,
      Long requesterId,
      String status
  ) {
    RequestCategory category = mock(RequestCategory.class);
    when(category.getId()).thenReturn(1L);
    when(category.getName()).thenReturn("Electricity/Lighting");
    Location location = mock(Location.class);
    when(location.getId()).thenReturn(2L);
    when(location.getName()).thenReturn("Student Center");
    ServiceRequest request = mock(ServiceRequest.class);
    when(request.getId()).thenReturn(id);
    lenient().when(request.isRequestedBy(requesterId)).thenReturn(true);
    when(request.getVisibility()).thenReturn(visibility);
    when(request.getReceiptNumber()).thenReturn("SR-20260801-0001");
    when(request.getTitle()).thenReturn("Flickering hallway light");
    when(request.getDescription()).thenReturn(
        "The hallway light keeps flickering."
    );
    when(request.getEquipmentName()).thenReturn("LED light");
    when(request.getRequestCategory()).thenReturn(category);
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

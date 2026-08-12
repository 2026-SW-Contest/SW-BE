package org.swbe.domain.facilityrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.swbe.domain.campus.entity.Building;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.file.entity.FileResource;
import org.swbe.domain.file.service.FilePublicUrlResolver;
import org.swbe.domain.facilityrequest.dto.response.AdminFacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.entity.FacilityCategory;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestAttachment;
import org.swbe.domain.facilityrequest.entity.RequestComment;
import org.swbe.domain.facilityrequest.exception.FacilityRequestErrorCode;
import org.swbe.domain.facilityrequest.repository.FacilityRequestAttachmentRepository;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.facilityrequest.repository.RequestCommentRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.global.error.BusinessException;

class AdminFacilityRequestDetailServiceTest {

  private FacilityRequestRepository facilityRequestRepository;
  private FacilityRequestAttachmentRepository attachmentRepository;
  private RequestCommentRepository requestCommentRepository;
  private FilePublicUrlResolver filePublicUrlResolver;
  private AdminFacilityRequestDetailService service;

  @BeforeEach
  void setUp() {
    facilityRequestRepository = mock(FacilityRequestRepository.class);
    attachmentRepository = mock(FacilityRequestAttachmentRepository.class);
    requestCommentRepository = mock(RequestCommentRepository.class);
    filePublicUrlResolver = mock(FilePublicUrlResolver.class);
    service = new AdminFacilityRequestDetailService(
        facilityRequestRepository,
        attachmentRepository,
        requestCommentRepository,
        filePublicUrlResolver
    );
  }

  @Test
  void returnsAdminFacilityRequestDetail() {
    FacilityRequest request = request();
    FileResource file = mock(FileResource.class);
    when(file.getId()).thenReturn(15L);
    when(file.getOriginalFilename()).thenReturn("broken-light.jpg");
    FacilityRequestAttachment attachment =
        mock(FacilityRequestAttachment.class);
    when(attachment.getFile()).thenReturn(file);
    RequestComment response = mock(RequestComment.class);
    when(response.getId()).thenReturn(3L);
    when(response.getContent()).thenReturn("Inspection started.");
    when(response.getCreatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 12, 15, 30)
    );
    when(facilityRequestRepository.findAdminDetailById(25L))
        .thenReturn(java.util.Optional.of(request));
    when(attachmentRepository
        .findAllByFacilityRequest_IdAndFile_DeletedAtIsNullOrderByIdAsc(25L))
        .thenReturn(List.of(attachment));
    when(requestCommentRepository
        .findAllByFacilityRequest_IdAndCommentTypeAndInternalFalseOrderByCreatedAtAscIdAsc(
            25L,
            "ADMIN_RESPONSE"
        ))
        .thenReturn(List.of(response));
    when(filePublicUrlResolver.resolve(file))
        .thenReturn("https://cdn.example.com/request-25.jpg");

    AdminFacilityRequestDetailResponse result =
        service.getFacilityRequest(25L);

    assertThat(result.data().facilityRequestId()).isEqualTo(25L);
    assertThat(result.data().requester().email())
        .isEqualTo("student@mju.ac.kr");
    assertThat(result.data().location().locationCode()).isEqualTo("S2");
    assertThat(result.data().attachments()).hasSize(1);
    assertThat(result.data().attachments().getFirst().fileUrl())
        .isEqualTo("https://cdn.example.com/request-25.jpg");
    assertThat(result.data().adminResponses()).hasSize(1);
    assertThat(result.data().adminResponses().getFirst().content())
        .isEqualTo("Inspection started.");
  }

  @Test
  void missingFacilityRequestIsRejected() {
    when(facilityRequestRepository.findAdminDetailById(99L))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> service.getFacilityRequest(99L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(FacilityRequestErrorCode.NOT_FOUND)
        );
  }

  private FacilityRequest request() {
    AppUser requester = mock(AppUser.class);
    when(requester.getId()).thenReturn(7L);
    when(requester.getName()).thenReturn("Hong");
    when(requester.getStudentNumber()).thenReturn("60241234");
    when(requester.getEmail()).thenReturn("student@mju.ac.kr");
    FacilityCategory category = mock(FacilityCategory.class);
    when(category.getId()).thenReturn(1L);
    when(category.getName()).thenReturn("Lighting");
    Building building = mock(Building.class);
    when(building.getCode()).thenReturn("S2");
    Location location = mock(Location.class);
    when(location.getId()).thenReturn(2L);
    when(location.getName()).thenReturn("Student Hall");
    when(location.getBuilding()).thenReturn(building);
    FacilityRequest request = mock(FacilityRequest.class);
    when(request.getId()).thenReturn(25L);
    when(request.getTitle()).thenReturn("Hallway light issue");
    when(request.getDescription()).thenReturn(
        "The hallway light keeps flickering."
    );
    when(request.getRequester()).thenReturn(requester);
    when(request.getFacilityCategory()).thenReturn(category);
    when(request.getLocation()).thenReturn(location);
    when(request.getRequestStatus()).thenReturn("IN_PROGRESS");
    when(request.getCreatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 12, 14, 30)
    );
    when(request.getUpdatedAt()).thenReturn(
        LocalDateTime.of(2026, 8, 12, 15, 30)
    );
    return request;
  }
}

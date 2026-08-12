package org.swbe.domain.facilityrequest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestCreateDataResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestCreateResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityCategoryResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailDataResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestLocationDetailResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestPageResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestUpdateDataResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestUpdateResponse;
import org.swbe.domain.facilityrequest.service.FacilityRequestDetailService;
import org.swbe.domain.facilityrequest.service.FacilityRequestDeleteService;
import org.swbe.domain.facilityrequest.service.FacilityRequestCreateService;
import org.swbe.domain.facilityrequest.service.FacilityRequestQueryService;
import org.swbe.domain.facilityrequest.service.FacilityRequestUpdateService;
import org.swbe.domain.user.entity.AccountStatus;
import org.swbe.global.security.AppUserPrincipal;
import org.swbe.global.security.RestAccessDeniedHandler;
import org.swbe.global.security.RestAuthenticationEntryPoint;
import org.swbe.global.security.RestSessionInformationExpiredStrategy;
import org.swbe.global.security.SecurityConfig;
import org.swbe.global.security.SecurityErrorResponseWriter;

@WebMvcTest(FacilityRequestController.class)
@Import({
    SecurityConfig.class,
    SecurityErrorResponseWriter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    RestSessionInformationExpiredStrategy.class
})
@TestPropertySource(properties = {
    "app.security.frontend-origins[0]=http://localhost:3000"
})
class FacilityRequestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FacilityRequestQueryService facilityRequestQueryService;

  @MockitoBean
  private FacilityRequestDetailService facilityRequestDetailService;

  @MockitoBean
  private FacilityRequestCreateService facilityRequestCreateService;

  @MockitoBean
  private FacilityRequestDeleteService facilityRequestDeleteService;

  @MockitoBean
  private FacilityRequestUpdateService facilityRequestUpdateService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void studentCanCreateFacilityRequestWithImages() throws Exception {
    MockMultipartFile requestPart = requestPart(
        """
            {
              "categoryId": 1,
              "locationId": 2,
              "title": "Flickering hallway light",
              "description": "The hallway light keeps flickering."
            }
            """
    );
    MockMultipartFile image = new MockMultipartFile(
        "files",
        "broken-light.jpg",
        "image/jpeg",
        "image".getBytes(StandardCharsets.UTF_8)
    );
    FacilityRequestCreateDataResponse data =
        new FacilityRequestCreateDataResponse(
            25L,
            "WAITING",
            1,
            LocalDateTime.of(2026, 8, 1, 16, 0)
        );
    when(facilityRequestCreateService.create(
        any(),
        any(),
        eq(7L)
    )).thenReturn(new FacilityRequestCreateResponse(data));

    mockMvc.perform(multipart("/api/facility-requests")
            .file(requestPart)
            .file(image)
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.facilityRequestId").value(25))
        .andExpect(jsonPath("$.data.receiptNumber").doesNotExist())
        .andExpect(jsonPath("$.data.requestStatus").value("WAITING"))
        .andExpect(jsonPath("$.data.attachmentCount").value(1));
  }

  @Test
  void anonymousUserCannotCreateFacilityRequest() throws Exception {
    mockMvc.perform(multipart("/api/facility-requests")
            .file(requestPart(validRequestJson()))
            .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void nonStudentCannotCreateFacilityRequest() throws Exception {
    mockMvc.perform(multipart("/api/facility-requests")
            .file(requestPart(validRequestJson()))
            .with(user(principal("ROLE_FACILITY_STAFF")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void blankTitleIsRejected() throws Exception {
    MockMultipartFile requestPart = requestPart(
        """
            {
              "categoryId": 1,
              "locationId": 2,
              "title": "   ",
              "description": "The hallway light keeps flickering."
            }
            """
    );

    mockMvc.perform(multipart("/api/facility-requests")
            .file(requestPart)
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void anonymousUserCanGetFilteredFacilityRequestList() throws Exception {
    FacilityRequestListItemResponse item = new FacilityRequestListItemResponse(
        25L,
        "학생회관 1층 조명 깜빡임",
        "전기/조명",
        "학생회관",
        "IN_PROGRESS",
        "진행중",
        null,
        LocalDateTime.of(2026, 8, 1, 16, 0)
    );
    when(facilityRequestQueryService.getFacilityRequests(any())).thenReturn(
        new FacilityRequestListResponse(
            new FacilityRequestPageResponse(
                List.of(item),
                0,
                20,
                1,
                1,
                false
            )
        )
    );

    mockMvc.perform(get("/api/facility-requests")
            .queryParam("categoryId", "1")
            .queryParam("locationId", "2")
            .queryParam("status", "IN_PROGRESS")
            .queryParam("keyword", "조명")
            .queryParam("from", "2026-07-01")
            .queryParam("to", "2026-08-01")
            .queryParam("page", "0")
            .queryParam("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].facilityRequestId").value(25))
        .andExpect(jsonPath("$.data.content[0].title")
            .value("학생회관 1층 조명 깜빡임"))
        .andExpect(jsonPath("$.data.content[0].categoryName")
            .value("전기/조명"))
        .andExpect(jsonPath("$.data.content[0].locationName")
            .value("학생회관"))
        .andExpect(jsonPath("$.data.content[0].requestStatus")
            .value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.content[0].requestStatusName")
            .value("진행중"))
        .andExpect(jsonPath("$.data.content[0].thumbnailUrl").isEmpty())
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.totalPages").value(1))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void invalidPageSizeIsRejected() throws Exception {
    mockMvc.perform(get("/api/facility-requests")
            .queryParam("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
  }

  @Test
  void anonymousUserCanGetFacilityRequestDetail() throws Exception {
    FacilityRequestDetailDataResponse data = new FacilityRequestDetailDataResponse(
        25L,
        "Flickering hallway light",
        "The hallway light keeps flickering.",
        new FacilityCategoryResponse(1L, "Electricity/Lighting"),
        new FacilityRequestLocationDetailResponse(2L, "Student Center"),
        "IN_PROGRESS",
        "In progress",
        List.of(),
        false,
        false,
        LocalDateTime.of(2026, 8, 1, 16, 0),
        LocalDateTime.of(2026, 8, 1, 16, 10)
    );
    when(facilityRequestDetailService.getFacilityRequest(25L, null))
        .thenReturn(new FacilityRequestDetailResponse(data));

    mockMvc.perform(get("/api/facility-requests/25"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.facilityRequestId").value(25))
        .andExpect(jsonPath("$.data.receiptNumber").doesNotExist())
        .andExpect(jsonPath("$.data.equipmentName").doesNotExist())
        .andExpect(jsonPath("$.data.category.categoryId").value(1))
        .andExpect(jsonPath("$.data.location.locationId").value(2))
        .andExpect(jsonPath("$.data.attachments.length()").value(0))
        .andExpect(jsonPath("$.data.editable").value(false))
        .andExpect(jsonPath("$.data.deletable").value(false));
  }

  @Test
  void studentCanDeleteOwnReceivedFacilityRequest() throws Exception {
    mockMvc.perform(delete("/api/facility-requests/25")
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void anonymousUserCannotDeleteFacilityRequest() throws Exception {
    mockMvc.perform(delete("/api/facility-requests/25")
                        .with(csrf()))
      .andExpect(status().isUnauthorized());
}
  @Test
  void studentCanUpdateOwnReceivedFacilityRequest() throws Exception {
    MockMultipartFile requestPart = requestPart(
        """
            {
              "title": "Updated hallway light",
              "keepFileIds": [15]
            }
            """
    );
    MockMultipartFile image = new MockMultipartFile(
        "files",
        "updated-light.jpg",
        "image/jpeg",
        "image".getBytes(StandardCharsets.UTF_8)
    );
    FacilityRequestUpdateDataResponse data =
        new FacilityRequestUpdateDataResponse(
            25L,
            "WAITING",
            2,
            LocalDateTime.of(2026, 8, 9, 16, 30)
        );
    when(facilityRequestUpdateService.update(
        eq(25L),
        any(),
        any(),
        eq(7L)
    )).thenReturn(new FacilityRequestUpdateResponse(data));

    mockMvc.perform(multipart("/api/facility-requests/25")
            .file(requestPart)
            .file(image)
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.facilityRequestId").value(25))
        .andExpect(jsonPath("$.data.requestStatus").value("WAITING"))
        .andExpect(jsonPath("$.data.attachmentCount").value(2));
  }

  @Test
  void anonymousUserCannotUpdateFacilityRequest() throws Exception {
    mockMvc.perform(multipart("/api/facility-requests/25")
            .file(requestPart("{\"title\":\"Updated title\"}"))
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void nonStudentCannotDeleteFacilityRequest() throws Exception {
    mockMvc.perform(delete("/api/facility-requests/25")
                      .with(user(principal("ROLE_FACILITY_STAFF")))
          .with(csrf()))
      .andExpect(status().isForbidden());
}

  @Test
  void nonStudentCannotUpdateFacilityRequest() throws Exception {
    mockMvc.perform(multipart("/api/facility-requests/25")
            .file(requestPart("{\"title\":\"Updated title\"}"))
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .with(user(principal("ROLE_FACILITY_STAFF")))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void blankUpdateTitleIsRejected() throws Exception {
    mockMvc.perform(multipart("/api/facility-requests/25")
            .file(requestPart("{\"title\":\"   \"}"))
            .with(request -> {
              request.setMethod("PATCH");
              return request;
            })
            .with(user(principal("ROLE_STUDENT")))
            .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
  }

  private MockMultipartFile requestPart(String json) {
    return new MockMultipartFile(
        "request",
        "",
        "application/json",
        json.getBytes(StandardCharsets.UTF_8)
    );
  }

  private String validRequestJson() {
    return """
        {
          "categoryId": 1,
          "locationId": 2,
          "title": "Flickering hallway light",
          "description": "The hallway light keeps flickering."
        }
        """;
  }

  private AppUserPrincipal principal(String authority) {
    return new AppUserPrincipal(
        7L,
        "student@mju.ac.kr",
        "{noop}password",
        AccountStatus.ACTIVE,
        true,
        List.of(new SimpleGrantedAuthority(authority))
    );
  }
}

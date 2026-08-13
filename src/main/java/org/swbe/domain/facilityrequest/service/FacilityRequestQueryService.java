package org.swbe.domain.facilityrequest.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.cursor.FacilityRequestCursor;
import org.swbe.domain.facilityrequest.cursor.FacilityRequestCursorCodec;
import org.swbe.domain.facilityrequest.dto.request.FacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestSliceResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

@Service
@Transactional(readOnly = true)
public class FacilityRequestQueryService {

  private final FacilityRequestRepository facilityRequestRepository;
  private final FacilityRequestThumbnailService thumbnailService;
  private final FacilityRequestCursorCodec cursorCodec;

  public FacilityRequestQueryService(
      FacilityRequestRepository facilityRequestRepository,
      FacilityRequestThumbnailService thumbnailService,
      FacilityRequestCursorCodec cursorCodec
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.thumbnailService = thumbnailService;
    this.cursorCodec = cursorCodec;
  }

  public FacilityRequestListResponse getFacilityRequests(
      FacilityRequestSearchCondition condition
  ) {
    validateDateRange(condition);

    LocalDateTime fromDateTime = condition.from() == null
        ? null
        : condition.from().atStartOfDay();
    LocalDateTime toDateTimeExclusive = condition.to() == null
        ? null
        : condition.to().plusDays(1).atStartOfDay();
    String status = condition.status() == null
        ? null
        : condition.status().name();
    FacilityRequestCursor cursor = condition.cursor() == null
        ? null
        : cursorCodec.decode(condition.cursor());

    List<FacilityRequest> matches =
        facilityRequestRepository.searchRequestsByCursor(
            condition.categoryId(),
            condition.locationId(),
            status,
            condition.keyword(),
            fromDateTime,
            toDateTimeExclusive,
            cursor == null ? null : cursor.createdAt(),
            cursor == null ? null : cursor.id(),
            PageRequest.of(0, condition.size() + 1)
        );
    boolean hasNext = matches.size() > condition.size();
    List<FacilityRequest> requests = hasNext
        ? matches.subList(0, condition.size())
        : matches;
    Map<Long, String> thumbnailUrls = requests.isEmpty()
        ? Map.of()
        : thumbnailService.resolveAll(
            requests.stream().map(FacilityRequest::getId).toList()
        );
    List<FacilityRequestListItemResponse> content = requests.stream()
        .map(request -> toListItemResponse(
            request,
            thumbnailUrls.get(request.getId())
        ))
        .toList();
    FacilityRequestSliceResponse slice = new FacilityRequestSliceResponse(
        content,
        nextCursor(requests, hasNext),
        hasNext
    );

    return new FacilityRequestListResponse(slice);
  }

  private String nextCursor(
      List<FacilityRequest> requests,
      boolean hasNext
  ) {
    if (!hasNext || requests.isEmpty()) {
      return null;
    }
    FacilityRequest lastRequest = requests.getLast();
    return cursorCodec.encode(
        lastRequest.getCreatedAt(),
        lastRequest.getId()
    );
  }

  private FacilityRequestListItemResponse toListItemResponse(
      FacilityRequest request,
      String thumbnailUrl
  ) {
    FacilityRequestStatus status = FacilityRequestStatus.valueOf(
        request.getRequestStatus()
    );

    return new FacilityRequestListItemResponse(
        request.getId(),
        request.getTitle(),
        request.getFacilityCategory().getName(),
        request.getLocation().getName(),
        request.getRequestStatus(),
        status.getDisplayName(),
        thumbnailUrl,
        request.getCreatedAt()
    );
  }

  private void validateDateRange(FacilityRequestSearchCondition condition) {
    if (condition.from() != null
        && condition.to() != null
        && condition.from().isAfter(condition.to())) {
      throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
    }
  }
}

package org.swbe.domain.facilityrequest.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.dto.response.FacilityRequestListItemResponse;
import org.swbe.domain.facilityrequest.cursor.FacilityRequestCursor;
import org.swbe.domain.facilityrequest.cursor.FacilityRequestCursorCodec;
import org.swbe.domain.facilityrequest.dto.response.MyFacilityRequestListResponse;
import org.swbe.domain.facilityrequest.dto.response.MyFacilityRequestSliceResponse;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;

@Service
@Transactional(readOnly = true)
public class MyFacilityRequestQueryService {

  private final FacilityRequestRepository facilityRequestRepository;
  private final FacilityRequestThumbnailService thumbnailService;
  private final FacilityRequestCursorCodec cursorCodec;

  public MyFacilityRequestQueryService(
      FacilityRequestRepository facilityRequestRepository,
      FacilityRequestThumbnailService thumbnailService,
      FacilityRequestCursorCodec cursorCodec
  ) {
    this.facilityRequestRepository = facilityRequestRepository;
    this.thumbnailService = thumbnailService;
    this.cursorCodec = cursorCodec;
  }

  // 로그인한 학생이 작성한 모든 시설 문의를 최신 등록순으로 조회한다.
  public MyFacilityRequestListResponse getMyFacilityRequests(
      Long requesterUserId,
      String encodedCursor,
      int size
  ) {
    FacilityRequestCursor cursor = encodedCursor == null
        ? null
        : cursorCodec.decode(encodedCursor);
    List<FacilityRequest> matches = facilityRequestRepository
        .findAllByRequesterIdAndCursor(
            requesterUserId,
            cursor == null ? null : cursor.createdAt(),
            cursor == null ? null : cursor.id(),
            PageRequest.of(0, size + 1)
        );
    boolean hasNext = matches.size() > size;
    List<FacilityRequest> requests = hasNext
        ? matches.subList(0, size)
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
    return new MyFacilityRequestListResponse(
        new MyFacilityRequestSliceResponse(
            content,
            nextCursor(requests, hasNext),
            hasNext
        )
    );
  }

  // 다음 목록이 있을 때 마지막 문의의 등록 시각과 ID로 커서를 만든다.
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

  // 시설 문의 엔터티를 마이페이지 목록에 필요한 간략한 정보로 변환한다.
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
}

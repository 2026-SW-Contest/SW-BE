package org.swbe.domain.search.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.FacilityRequestStatus;
import org.swbe.domain.facilityrequest.repository.FacilityRequestRepository;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.domain.search.cursor.SearchCursor;
import org.swbe.domain.search.cursor.SearchCursorCodec;
import org.swbe.domain.search.dto.response.CursorSliceResponse;
import org.swbe.domain.search.dto.response.FacilityRequestSearchItemResponse;
import org.swbe.domain.search.dto.response.FacilityRequestSearchResponse;
import org.swbe.domain.search.dto.response.LostItemSearchItemResponse;
import org.swbe.domain.search.dto.response.LostItemSearchResponse;
import org.swbe.domain.search.dto.response.SearchSummaryDataResponse;
import org.swbe.domain.search.dto.response.SearchSummaryResponse;
import org.swbe.domain.search.support.SearchKeyword;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntegratedSearchService {

  private final StoredItemRepository storedItemRepository;
  private final FacilityRequestRepository facilityRequestRepository;
  private final SearchCursorCodec searchCursorCodec;

  public SearchSummaryResponse getSummary(String rawKeyword) {
    SearchKeyword keyword = SearchKeyword.from(rawKeyword);

    return new SearchSummaryResponse(
        new SearchSummaryDataResponse(
            keyword.value(),
            storedItemRepository.countSearchMatches(
                keyword.containsPattern()
            ),
            facilityRequestRepository
                .countIntegratedSearchMatches(
                    keyword.containsPattern()
                )
        )
    );
  }

  public LostItemSearchResponse searchLostItems(
      String rawKeyword,
      String encodedCursor,
      int size
  ) {
    SearchKeyword keyword = SearchKeyword.from(rawKeyword);
    SearchCursor cursor = decodeNullable(encodedCursor);

    List<StoredItem> matches = storedItemRepository.searchByCursor(
        keyword.containsPattern(),
        cursor == null ? null : cursor.createdAt(),
        cursor == null ? null : cursor.id(),
        PageRequest.of(0, size + 1)
    );
    boolean hasNext = matches.size() > size;
    List<StoredItem> content = hasNext
        ? matches.subList(0, size)
        : matches;
    List<LostItemSearchItemResponse> responses = content.stream()
        .map(this::toLostItemResponse)
        .toList();

    return new LostItemSearchResponse(
        new CursorSliceResponse<>(
            responses,
            nextCursor(content, hasNext),
            hasNext
        )
    );
  }

  public FacilityRequestSearchResponse searchFacilityRequests(
      String rawKeyword,
      String encodedCursor,
      int size
  ) {
    SearchKeyword keyword = SearchKeyword.from(rawKeyword);
    SearchCursor cursor = decodeNullable(encodedCursor);

    List<FacilityRequest> matches =
        facilityRequestRepository.searchIntegratedByCursor(
            keyword.containsPattern(),
            cursor == null ? null : cursor.createdAt(),
            cursor == null ? null : cursor.id(),
            PageRequest.of(0, size + 1)
        );
    boolean hasNext = matches.size() > size;
    List<FacilityRequest> content = hasNext
        ? matches.subList(0, size)
        : matches;
    List<FacilityRequestSearchItemResponse> responses =
        content.stream()
            .map(this::toFacilityRequestResponse)
            .toList();

    return new FacilityRequestSearchResponse(
        new CursorSliceResponse<>(
            responses,
            nextCursorForFacilityRequests(content, hasNext),
            hasNext
        )
    );
  }

  private SearchCursor decodeNullable(String encodedCursor) {
    return encodedCursor == null
        ? null
        : searchCursorCodec.decode(encodedCursor);
  }

  private String nextCursor(
      List<StoredItem> content,
      boolean hasNext
  ) {
    if (!hasNext) {
      return null;
    }

    StoredItem last = content.getLast();
    return searchCursorCodec.encode(
        last.getCreatedAt(),
        last.getId()
    );
  }

  private String nextCursorForFacilityRequests(
      List<FacilityRequest> content,
      boolean hasNext
  ) {
    if (!hasNext) {
      return null;
    }

    FacilityRequest last = content.getLast();
    return searchCursorCodec.encode(
        last.getCreatedAt(),
        last.getId()
    );
  }

  private LostItemSearchItemResponse toLostItemResponse(
      StoredItem item
  ) {
    return new LostItemSearchItemResponse(
        item.getId(),
        item.getItemName(),
        item.getItemCategory().getName(),
        item.getPublicDescription(),
        item.getFoundLocation() == null
            ? null
            : item.getFoundLocation().getName(),
        item.getFoundDate(),
        item.getPublicStatus(),
        null,
        item.getCreatedAt()
    );
  }

  private FacilityRequestSearchItemResponse
      toFacilityRequestResponse(FacilityRequest request) {
    FacilityRequestStatus status =
        FacilityRequestStatus.valueOf(
            request.getRequestStatus()
        );

    return new FacilityRequestSearchItemResponse(
        request.getId(),
        request.getTitle(),
        request.getDescription(),
        request.getEquipmentName(),
        request.getFacilityCategory().getName(),
        request.getLocation().getName(),
        request.getRequestStatus(),
        status.getDisplayName(),
        null,
        request.getCreatedAt()
    );
  }
}

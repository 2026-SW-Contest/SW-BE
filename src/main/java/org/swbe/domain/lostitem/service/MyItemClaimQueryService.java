package org.swbe.domain.lostitem.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.lostitem.cursor.ItemClaimCursor;
import org.swbe.domain.lostitem.cursor.ItemClaimCursorCodec;
import org.swbe.domain.lostitem.dto.response.MyItemClaimListItemResponse;
import org.swbe.domain.lostitem.dto.response.MyItemClaimListResponse;
import org.swbe.domain.lostitem.dto.response.MyItemClaimSliceResponse;
import org.swbe.domain.lostitem.entity.ItemClaim;
import org.swbe.domain.lostitem.entity.ItemClaimStatus;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.repository.ItemClaimRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyItemClaimQueryService {

  private final ItemClaimRepository itemClaimRepository;
  private final StoredItemThumbnailService thumbnailService;
  private final ItemClaimCursorCodec cursorCodec;

  // 로그인한 학생이 등록한 소유자 확인 요청을 최신 신청순으로 조회한다.
  public MyItemClaimListResponse getMyItemClaims(
      Long claimantUserId,
      String encodedCursor,
      int size
  ) {
    ItemClaimCursor cursor = encodedCursor == null
        ? null
        : cursorCodec.decode(encodedCursor);
    List<ItemClaim> matches = itemClaimRepository
        .findAllByClaimantUserIdAndCursor(
            claimantUserId,
            cursor == null ? null : cursor.createdAt(),
            cursor == null ? null : cursor.id(),
            PageRequest.of(0, size + 1)
        );
    boolean hasNext = matches.size() > size;
    List<ItemClaim> content = hasNext
        ? matches.subList(0, size)
        : matches;
    List<Long> storedItemIds = content.stream()
        .map(claim -> claim.getStoredItem().getId())
        .distinct()
        .toList();
    Map<Long, String> thumbnailUrls = storedItemIds.isEmpty()
        ? Map.of()
        : thumbnailService.resolveAll(storedItemIds);
    List<MyItemClaimListItemResponse> responses = content.stream()
        .map(claim -> toListItemResponse(
            claim,
            thumbnailUrls.get(claim.getStoredItem().getId())
        ))
        .toList();

    return new MyItemClaimListResponse(
        new MyItemClaimSliceResponse(
            responses,
            nextCursor(content, hasNext),
            hasNext
        )
    );
  }

  // 요청 엔터티와 대상 분실물 정보를 마이페이지 목록 응답으로 변환한다.
  private MyItemClaimListItemResponse toListItemResponse(
      ItemClaim claim,
      String thumbnailUrl
  ) {
    StoredItem storedItem = claim.getStoredItem();
    ItemClaimStatus status = claim.getClaimStatus();

    return new MyItemClaimListItemResponse(
        claim.getId(),
        storedItem.getId(),
        storedItem.getItemName(),
        storedItem.getItemCategory().getName(),
        storedItem.getFoundLocationName(),
        storedItem.getFoundDate(),
        claim.getRequestMethod(),
        status.name(),
        status.getDisplayName(),
        thumbnailUrl,
        claim.getDecisionMessage(),
        claim.getCreatedAt(),
        claim.getDecidedAt()
    );
  }

  // 다음 목록이 있을 때 마지막 요청의 생성 시각과 ID로 커서를 만든다.
  private String nextCursor(List<ItemClaim> content, boolean hasNext) {
    if (!hasNext || content.isEmpty()) {
      return null;
    }
    ItemClaim lastClaim = content.getLast();
    return cursorCodec.encode(lastClaim.getCreatedAt(), lastClaim.getId());
  }
}

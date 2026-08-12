package org.swbe.domain.lostitem.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.lostitem.cursor.StoredItemCursor;
import org.swbe.domain.lostitem.cursor.StoredItemCursorCodec;
import org.swbe.domain.lostitem.dto.request.StoredItemSearchCondition;
import org.swbe.domain.lostitem.dto.response.StoredItemListItemResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemListResponse;
import org.swbe.domain.lostitem.dto.response.StoredItemSliceResponse;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.repository.StoredItemRepository;
import org.swbe.global.error.BusinessException;
import org.swbe.global.error.CommonErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoredItemQueryService {

  private final StoredItemRepository storedItemRepository;
  private final StoredItemCursorCodec cursorCodec;
  private final StoredItemThumbnailService thumbnailService;

  public StoredItemListResponse getStoredItems(
      StoredItemSearchCondition condition
  ) {
    validateDateRange(condition);
    StoredItemCursor cursor = condition.cursor() == null
        ? null
        : cursorCodec.decode(condition.cursor());
    List<StoredItem> matches = storedItemRepository.findAllByCursor(
        condition.categoryId(),
        condition.locationId(),
        condition.status(),
        condition.from(),
        condition.to(),
        cursor == null ? null : cursor.createdAt(),
        cursor == null ? null : cursor.id(),
        condition.size() + 1
    );
    boolean hasNext = matches.size() > condition.size();
    List<StoredItem> content = hasNext
        ? matches.subList(0, condition.size())
        : matches;
    Map<Long, String> thumbnailUrls = content.isEmpty()
        ? Map.of()
        : thumbnailService.resolveAll(
            content.stream().map(StoredItem::getId).toList()
        );
    List<StoredItemListItemResponse> responses = content.stream()
        .map(item -> toListItemResponse(
            item,
            thumbnailUrls.get(item.getId())
        ))
        .toList();

    return new StoredItemListResponse(
        new StoredItemSliceResponse(
            responses,
            nextCursor(content, hasNext),
            hasNext
        )
    );
  }

  private StoredItemListItemResponse toListItemResponse(
      StoredItem item,
      String thumbnailUrl
  ) {
    return new StoredItemListItemResponse(
        item.getId(),
        item.getItemName(),
        item.getPublicDescription(),
        item.getItemCategory().getName(),
        item.getFoundLocationName(),
        item.getFoundDate(),
        item.getPublicStatus().name(),
        item.getPublicStatus().getDisplayName(),
        thumbnailUrl,
        item.getCreatedAt()
    );
  }

  private String nextCursor(
      List<StoredItem> content,
      boolean hasNext
  ) {
    if (!hasNext) {
      return null;
    }

    StoredItem last = content.getLast();
    return cursorCodec.encode(last.getCreatedAt(), last.getId());
  }

  private void validateDateRange(StoredItemSearchCondition condition) {
    if (condition.from() != null
        && condition.to() != null
        && condition.from().isAfter(condition.to())) {
      throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
    }
  }
}

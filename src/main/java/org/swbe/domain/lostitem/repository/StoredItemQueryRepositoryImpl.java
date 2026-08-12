package org.swbe.domain.lostitem.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.swbe.domain.campus.entity.QLocation;
import org.swbe.domain.lostitem.entity.QItemCategory;
import org.swbe.domain.lostitem.entity.QStoredItem;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemStatus;

@RequiredArgsConstructor
public class StoredItemQueryRepositoryImpl
    implements StoredItemQueryRepository {

  private static final QStoredItem ITEM = QStoredItem.storedItem;
  private static final QItemCategory CATEGORY = QItemCategory.itemCategory;
  private static final QLocation LOCATION = QLocation.location;

  private final JPAQueryFactory queryFactory;

  @Override
  public List<StoredItem> findAllByCursor(
      Long categoryId,
      Long locationId,
      StoredItemStatus status,
      LocalDate from,
      LocalDate to,
      LocalDateTime cursorCreatedAt,
      Long cursorId,
      int limit
  ) {
    return queryFactory
        .selectFrom(ITEM)
        .join(ITEM.itemCategory, CATEGORY).fetchJoin()
        .leftJoin(ITEM.foundLocation, LOCATION).fetchJoin()
        .where(
            categoryIdEq(categoryId),
            locationIdEq(locationId),
            statusEq(status),
            foundDateGoe(from),
            foundDateLoe(to),
            beforeCursor(cursorCreatedAt, cursorId)
        )
        .orderBy(ITEM.createdAt.desc(), ITEM.id.desc())
        .limit(limit)
        .fetch();
  }

  private BooleanExpression categoryIdEq(Long categoryId) {
    return categoryId == null ? null : CATEGORY.id.eq(categoryId);
  }

  private BooleanExpression locationIdEq(Long locationId) {
    return locationId == null ? null : LOCATION.id.eq(locationId);
  }

  private BooleanExpression statusEq(StoredItemStatus status) {
    return status == null ? null : ITEM.publicStatus.eq(status);
  }

  private BooleanExpression foundDateGoe(LocalDate from) {
    return from == null ? null : ITEM.foundDate.goe(from);
  }

  private BooleanExpression foundDateLoe(LocalDate to) {
    return to == null ? null : ITEM.foundDate.loe(to);
  }

  private BooleanExpression beforeCursor(
      LocalDateTime cursorCreatedAt,
      Long cursorId
  ) {
    if (cursorCreatedAt == null || cursorId == null) {
      return null;
    }

    return ITEM.createdAt.lt(cursorCreatedAt)
        .or(
            ITEM.createdAt.eq(cursorCreatedAt)
                .and(ITEM.id.lt(cursorId))
        );
  }
}

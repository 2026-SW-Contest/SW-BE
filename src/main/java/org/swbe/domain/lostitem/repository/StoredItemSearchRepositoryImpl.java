package org.swbe.domain.lostitem.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.campus.entity.QLocation;
import org.swbe.domain.lostitem.entity.QItemCategory;
import org.swbe.domain.lostitem.entity.QStoredItem;
import org.swbe.domain.lostitem.entity.StoredItem;

@RequiredArgsConstructor
public class StoredItemSearchRepositoryImpl
    implements StoredItemSearchRepository {

  private static final QStoredItem ITEM = QStoredItem.storedItem;
  private static final QItemCategory CATEGORY = QItemCategory.itemCategory;
  private static final QLocation LOCATION = QLocation.location;

  private final JPAQueryFactory queryFactory;

  @Override
  public long countSearchMatches(String pattern) {
    Long count = queryFactory
        .select(ITEM.count())
        .from(ITEM)
        .join(ITEM.itemCategory, CATEGORY)
        .leftJoin(ITEM.foundLocation, LOCATION)
        .where(matchesKeyword(pattern))
        .fetchOne();

    return count == null ? 0L : count;
  }

  @Override
  public List<StoredItem> searchByCursor(
      String pattern,
      LocalDateTime cursorCreatedAt,
      Long cursorId,
      Pageable pageable
  ) {
    return queryFactory
        .selectFrom(ITEM)
        .join(ITEM.itemCategory, CATEGORY).fetchJoin()
        .leftJoin(ITEM.foundLocation, LOCATION).fetchJoin()
        .where(
            matchesKeyword(pattern),
            beforeCursor(cursorCreatedAt, cursorId)
        )
        .orderBy(ITEM.createdAt.desc(), ITEM.id.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  private BooleanExpression matchesKeyword(String pattern) {
    return ITEM.itemName.lower().like(pattern, '!')
        .or(ITEM.publicDescription.lower().like(pattern, '!'))
        .or(CATEGORY.name.lower().like(pattern, '!'))
        .or(LOCATION.name.lower().like(pattern, '!'));
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

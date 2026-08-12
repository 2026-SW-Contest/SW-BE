package org.swbe.domain.lostitem.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.swbe.domain.lostitem.entity.QStoredItem;
import org.swbe.domain.lostitem.entity.StoredItem;
import org.swbe.domain.lostitem.entity.StoredItemStatus;

class StoredItemQueryRepositoryImplTest {

  private static final QStoredItem ITEM = QStoredItem.storedItem;

  @Test
  void buildsDynamicFiltersAndStableKeysetOrder() {
    JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
    @SuppressWarnings("unchecked")
    JPAQuery<StoredItem> query = mock(
        JPAQuery.class,
        Answers.RETURNS_SELF
    );
    when(queryFactory.selectFrom(ITEM)).thenReturn(query);
    when(query.fetch()).thenReturn(List.of());
    StoredItemQueryRepositoryImpl repository =
        new StoredItemQueryRepositoryImpl(queryFactory);
    LocalDateTime cursorCreatedAt = LocalDateTime.of(
        2026,
        8,
        12,
        14,
        30
    );

    repository.findAllByCursor(
        2L,
        10L,
        StoredItemStatus.STORED,
        LocalDate.of(2026, 8, 1),
        LocalDate.of(2026, 8, 12),
        cursorCreatedAt,
        25L,
        21
    );

    verify(query).orderBy(ITEM.createdAt.desc(), ITEM.id.desc());
    verify(query).limit(21L);
    ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(
        Predicate[].class
    );
    verify(query).where(predicates.capture());
    assertThat(predicates.getValue())
        .hasSize(6)
        .allMatch(predicate -> predicate != null);
    assertThat(predicates.getValue()[5].toString())
        .contains("storedItem.createdAt < 2026-08-12T14:30")
        .contains("storedItem.createdAt = 2026-08-12T14:30")
        .contains("storedItem.id < 25");
  }

  @Test
  void omitsAbsentFiltersAndCursor() {
    JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
    @SuppressWarnings("unchecked")
    JPAQuery<StoredItem> query = mock(
        JPAQuery.class,
        Answers.RETURNS_SELF
    );
    when(queryFactory.selectFrom(ITEM)).thenReturn(query);
    when(query.fetch()).thenReturn(List.of());
    StoredItemQueryRepositoryImpl repository =
        new StoredItemQueryRepositoryImpl(queryFactory);

    repository.findAllByCursor(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        21
    );

    ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(
        Predicate[].class
    );
    verify(query).where(predicates.capture());
    assertThat(predicates.getValue())
        .hasSize(6)
        .allMatch(predicate -> predicate == null);
    verify(query).orderBy(ITEM.createdAt.desc(), ITEM.id.desc());
  }
}

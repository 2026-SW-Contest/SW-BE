package org.swbe.domain.facilityrequest.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.campus.entity.QLocation;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.QFacilityCategory;
import org.swbe.domain.facilityrequest.entity.QFacilityRequest;

@RequiredArgsConstructor
public class FacilityRequestIntegratedSearchRepositoryImpl
    implements FacilityRequestIntegratedSearchRepository {

  private static final QFacilityRequest REQUEST = QFacilityRequest.facilityRequest;
  private static final QFacilityCategory CATEGORY = QFacilityCategory.facilityCategory;
  private static final QLocation LOCATION = QLocation.location;

  private final JPAQueryFactory queryFactory;

  @Override
  public long countIntegratedSearchMatches(String pattern) {
    Long count = queryFactory
        .select(REQUEST.count())
        .from(REQUEST)
        .join(REQUEST.facilityCategory, CATEGORY)
        .join(REQUEST.location, LOCATION)
        .where(matchesKeyword(pattern))
        .fetchOne();

    return count == null ? 0L : count;
  }

  @Override
  public List<FacilityRequest> searchIntegratedByCursor(
      String pattern,
      LocalDateTime cursorCreatedAt,
      Long cursorId,
      Pageable pageable
  ) {
    return queryFactory
        .selectFrom(REQUEST)
        .join(REQUEST.facilityCategory, CATEGORY).fetchJoin()
        .join(REQUEST.location, LOCATION).fetchJoin()
        .where(
            matchesKeyword(pattern),
            beforeCursor(cursorCreatedAt, cursorId)
        )
        .orderBy(REQUEST.createdAt.desc(), REQUEST.id.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  private BooleanExpression matchesKeyword(String pattern) {
    return REQUEST.title.lower().like(pattern, '!')
        .or(REQUEST.description.lower().like(pattern, '!'))
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

    return REQUEST.createdAt.lt(cursorCreatedAt)
        .or(
            REQUEST.createdAt.eq(cursorCreatedAt)
                .and(REQUEST.id.lt(cursorId))
        );
  }
}

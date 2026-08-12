package org.swbe.domain.facilityrequest.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.swbe.domain.campus.entity.QBuilding;
import org.swbe.domain.campus.entity.QLocation;
import org.swbe.domain.facilityrequest.dto.request.AdminFacilityRequestSearchCondition;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;
import org.swbe.domain.facilityrequest.entity.QFacilityCategory;
import org.swbe.domain.facilityrequest.entity.QFacilityRequest;
import org.swbe.domain.user.entity.QAppUser;

@RequiredArgsConstructor
public class AdminFacilityRequestSearchRepositoryImpl
    implements AdminFacilityRequestSearchRepository {

  private static final QFacilityRequest REQUEST =
      QFacilityRequest.facilityRequest;
  private static final QFacilityCategory CATEGORY =
      QFacilityCategory.facilityCategory;
  private static final QLocation LOCATION = QLocation.location;
  private static final QBuilding BUILDING = QBuilding.building;
  private static final QAppUser REQUESTER = QAppUser.appUser;

  private final JPAQueryFactory queryFactory;

  // 관리자 검색 조건을 적용해 시설문의와 화면 표시 관계를 함께 조회한다.
  @Override
  public Page<FacilityRequest> searchAdminRequests(
      AdminFacilityRequestSearchCondition condition,
      Pageable pageable
  ) {
    LocalDateTime fromDateTime = condition.from() == null
        ? null
        : condition.from().atStartOfDay();
    LocalDateTime toDateTimeExclusive = condition.to() == null
        ? null
        : condition.to().plusDays(1).atStartOfDay();

    List<FacilityRequest> content = queryFactory
        .selectFrom(REQUEST)
        .join(REQUEST.facilityCategory, CATEGORY).fetchJoin()
        .join(REQUEST.location, LOCATION).fetchJoin()
        .join(LOCATION.building, BUILDING).fetchJoin()
        .join(REQUEST.requester, REQUESTER).fetchJoin()
        .where(
            matchesKeyword(condition.keyword()),
            statusEquals(condition),
            categoryEquals(condition.categoryId()),
            locationEquals(condition.locationId()),
            createdAtFrom(fromDateTime),
            createdAtBefore(toDateTimeExclusive)
        )
        .orderBy(REQUEST.createdAt.desc(), REQUEST.id.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    Long count = queryFactory
        .select(REQUEST.count())
        .from(REQUEST)
        .join(REQUEST.facilityCategory, CATEGORY)
        .join(REQUEST.location, LOCATION)
        .join(REQUEST.requester, REQUESTER)
        .where(
            matchesKeyword(condition.keyword()),
            statusEquals(condition),
            categoryEquals(condition.categoryId()),
            locationEquals(condition.locationId()),
            createdAtFrom(fromDateTime),
            createdAtBefore(toDateTimeExclusive)
        )
        .fetchOne();

    return new PageImpl<>(
        content,
        pageable,
        count == null ? 0L : count
    );
  }

  private BooleanExpression matchesKeyword(String keyword) {
    if (keyword == null) {
      return null;
    }

    return REQUEST.title.containsIgnoreCase(keyword)
        .or(REQUEST.description.containsIgnoreCase(keyword))
        .or(REQUESTER.name.containsIgnoreCase(keyword))
        .or(REQUESTER.studentNumber.containsIgnoreCase(keyword));
  }

  private BooleanExpression statusEquals(
      AdminFacilityRequestSearchCondition condition
  ) {
    return condition.status() == null
        ? null
        : REQUEST.requestStatus.eq(condition.status().name());
  }

  private BooleanExpression categoryEquals(Long categoryId) {
    return categoryId == null ? null : CATEGORY.id.eq(categoryId);
  }

  private BooleanExpression locationEquals(Long locationId) {
    return locationId == null ? null : LOCATION.id.eq(locationId);
  }

  private BooleanExpression createdAtFrom(LocalDateTime fromDateTime) {
    return fromDateTime == null
        ? null
        : REQUEST.createdAt.goe(fromDateTime);
  }

  private BooleanExpression createdAtBefore(
      LocalDateTime toDateTimeExclusive
  ) {
    return toDateTimeExclusive == null
        ? null
        : REQUEST.createdAt.lt(toDateTimeExclusive);
  }
}

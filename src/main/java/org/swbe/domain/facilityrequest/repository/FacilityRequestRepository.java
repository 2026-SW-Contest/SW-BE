package org.swbe.domain.facilityrequest.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.facilityrequest.entity.FacilityRequest;

public interface FacilityRequestRepository extends
    JpaRepository<FacilityRequest, Long>,
    FacilityRequestIntegratedSearchRepository,
    AdminFacilityRequestSearchRepository {

  @EntityGraph(attributePaths = {
      "facilityCategory",
      "location",
      "requester"
  })
  @Query("SELECT request FROM FacilityRequest request WHERE request.id = :id")
  Optional<FacilityRequest> findDetailById(@Param("id") Long id);

  @EntityGraph(attributePaths = {
      "facilityCategory",
      "location",
      "location.building",
      "requester"
  })
  @Query("SELECT request FROM FacilityRequest request WHERE request.id = :id")
  Optional<FacilityRequest> findAdminDetailById(@Param("id") Long id);

  @Query(
      value = """
          SELECT request
          FROM FacilityRequest request
          JOIN FETCH request.facilityCategory category
          JOIN FETCH request.location location
          WHERE (:categoryId IS NULL OR category.id = :categoryId)
            AND (:locationId IS NULL OR location.id = :locationId)
            AND (:status IS NULL OR request.requestStatus = :status)
            AND (
              :keyword IS NULL
              OR LOWER(request.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(request.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:fromDateTime IS NULL OR request.createdAt >= :fromDateTime)
            AND (:toDateTimeExclusive IS NULL OR request.createdAt < :toDateTimeExclusive)
          """,
      countQuery = """
          SELECT COUNT(request)
          FROM FacilityRequest request
          WHERE (:categoryId IS NULL OR request.facilityCategory.id = :categoryId)
            AND (:locationId IS NULL OR request.location.id = :locationId)
            AND (:status IS NULL OR request.requestStatus = :status)
            AND (
              :keyword IS NULL
              OR LOWER(request.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(request.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:fromDateTime IS NULL OR request.createdAt >= :fromDateTime)
            AND (:toDateTimeExclusive IS NULL OR request.createdAt < :toDateTimeExclusive)
          """
  )
  Page<FacilityRequest> searchRequests(
      @Param("categoryId") Long categoryId,
      @Param("locationId") Long locationId,
      @Param("status") String status,
      @Param("keyword") String keyword,
      @Param("fromDateTime") LocalDateTime fromDateTime,
      @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive,
      Pageable pageable
  );
}

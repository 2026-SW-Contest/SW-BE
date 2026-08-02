package org.swbe.domain.servicerequest.repository;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.swbe.domain.servicerequest.entity.ServiceRequest;

public interface ServiceRequestRepository
    extends JpaRepository<ServiceRequest, Long> {

  @Query(
      value = """
          SELECT request
          FROM ServiceRequest request
          JOIN FETCH request.requestCategory category
          JOIN FETCH request.location location
          WHERE request.visibility = 'PUBLIC'
            AND (:categoryId IS NULL OR category.id = :categoryId)
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
          FROM ServiceRequest request
          WHERE request.visibility = 'PUBLIC'
            AND (:categoryId IS NULL OR request.requestCategory.id = :categoryId)
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
  Page<ServiceRequest> searchPublicRequests(
      @Param("categoryId") Long categoryId,
      @Param("locationId") Long locationId,
      @Param("status") String status,
      @Param("keyword") String keyword,
      @Param("fromDateTime") LocalDateTime fromDateTime,
      @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive,
      Pageable pageable
  );
}

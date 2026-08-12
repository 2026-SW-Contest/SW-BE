package org.swbe.domain.facilityrequest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.swbe.domain.campus.entity.Location;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "facility_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FacilityRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "facility_request_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "facility_category_id")
  private FacilityCategory facilityCategory;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "location_id")
  private Location location;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "requester_user_id")
  private AppUser requester;

  @Column(nullable = false, length = 200)
  private String title;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(nullable = false)
  private String description;

  @Column(name = "request_status", nullable = false, length = 30)
  private String requestStatus = "WAITING";

  @Version
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  private FacilityRequest(
      FacilityCategory facilityCategory,
      Location location,
      AppUser requester,
      String title,
      String description,
      LocalDateTime createdAt
  ) {
    this.facilityCategory = Objects.requireNonNull(facilityCategory);
    this.location = Objects.requireNonNull(location);
    this.requester = Objects.requireNonNull(requester);
    this.title = requireText(title, "title");
    this.description = requireText(description, "description");
    this.requestStatus = FacilityRequestStatus.WAITING.name();
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = createdAt;
  }

  public static FacilityRequest create(
      FacilityCategory facilityCategory,
      Location location,
      AppUser requester,
      String title,
      String description,
      LocalDateTime createdAt
  ) {
    return new FacilityRequest(
        facilityCategory,
        location,
        requester,
        title,
        description,
        createdAt
    );
  }

  public boolean isRequestedBy(Long userId) {
    return userId != null && userId.equals(requester.getId());
  }

  // 문의가 아직 대기 상태여서 작성자가 삭제할 수 있는지 확인한다.
  public boolean isDeletable() {
    return FacilityRequestStatus.WAITING.name().equals(requestStatus);
  }

  // 문의가 아직 대기 상태여서 작성자가 수정할 수 있는지 확인한다.
  public boolean isEditable() {
    return FacilityRequestStatus.WAITING.name().equals(requestStatus);
  }

  // 전달된 값만 변경하고 마지막 수정 시각을 갱신한다.
  public void update(
      FacilityCategory facilityCategory,
      Location location,
      String title,
      String description,
      LocalDateTime updatedAt
  ) {
    if (facilityCategory != null) {
      this.facilityCategory = facilityCategory;
    }
    if (location != null) {
      this.location = location;
    }
    if (title != null) {
      this.title = requireText(title, "title");
    }
    if (description != null) {
      this.description = requireText(description, "description");
    }
    this.updatedAt = Objects.requireNonNull(updatedAt);
  }

  // 관리자가 요청한 상태로 현재 문의를 변경할 수 있는지 확인한다.
  public boolean canTransitionTo(FacilityRequestStatus nextStatus) {
    if (nextStatus == null) {
      return false;
    }
    FacilityRequestStatus currentStatus = FacilityRequestStatus.valueOf(
        requestStatus
    );

    return switch (currentStatus) {
      case RECEIVED -> nextStatus == FacilityRequestStatus.IN_PROGRESS
          || nextStatus == FacilityRequestStatus.COMPLETED;
      case IN_PROGRESS -> nextStatus == FacilityRequestStatus.COMPLETED;
      default -> false;
    };
  }

  // 관리자가 검증한 상태로 문의를 변경하고 처리 시각을 갱신한다.
  public void transitionTo(
      FacilityRequestStatus nextStatus,
      LocalDateTime updatedAt
  ) {
    if (!canTransitionTo(nextStatus)) {
      throw new IllegalStateException("Invalid facility request transition");
    }
    this.requestStatus = nextStatus.name();
    this.updatedAt = Objects.requireNonNull(updatedAt);
    if (nextStatus == FacilityRequestStatus.COMPLETED) {
      this.completedAt = updatedAt;
    }
  }

  // 답변만 등록된 경우에도 문의의 마지막 처리 시각을 갱신한다.
  public void touch(LocalDateTime updatedAt) {
    this.updatedAt = Objects.requireNonNull(updatedAt);
  }

  public boolean hasStatus(FacilityRequestStatus status) {
    return status != null && status.name().equals(requestStatus);
  }

  public boolean isCompleted() {
    return hasStatus(FacilityRequestStatus.COMPLETED);
  }

  private static String requireText(String value, String fieldName) {
    String stripped = stripNullable(value);
    if (stripped == null || stripped.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return stripped;
  }

  private static String stripNullable(String value) {
    return value == null ? null : value.strip();
  }
}

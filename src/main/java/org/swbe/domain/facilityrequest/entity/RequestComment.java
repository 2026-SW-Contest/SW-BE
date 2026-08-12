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
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "request_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestComment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "request_comment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "facility_request_id")
  private FacilityRequest facilityRequest;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_user_id")
  private AppUser author;

  @Column(name = "comment_type", nullable = false, length = 30)
  private String commentType;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(nullable = false)
  private String content;

  @Column(name = "is_internal", nullable = false)
  private boolean internal;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private RequestComment(
      FacilityRequest facilityRequest,
      AppUser author,
      String commentType,
      String content,
      boolean internal,
      LocalDateTime createdAt
  ) {
    this.facilityRequest = Objects.requireNonNull(facilityRequest);
    this.author = Objects.requireNonNull(author);
    this.commentType = requireText(commentType, "commentType");
    this.content = requireText(content, "content");
    this.internal = internal;
    this.createdAt = Objects.requireNonNull(createdAt);
  }

  // 사용자에게 공개할 관리자 시설문의 답변을 생성한다.
  public static RequestComment createAdminResponse(
      FacilityRequest facilityRequest,
      AppUser author,
      String content,
      LocalDateTime createdAt
  ) {
    return new RequestComment(
        facilityRequest,
        author,
        "ADMIN_RESPONSE",
        content,
        false,
        createdAt
    );
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }
}

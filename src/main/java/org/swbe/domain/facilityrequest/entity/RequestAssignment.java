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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.swbe.domain.campus.entity.Department;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "request_assignment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestAssignment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "request_assignment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "facility_request_id")
  private FacilityRequest facilityRequest;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assigned_department_id")
  private Department assignedDepartment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_user_id")
  private AppUser assignedUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_by")
  private AppUser assignedBy;

  @Column(name = "assigned_at", nullable = false)
  private LocalDateTime assignedAt;

  @Column(name = "ended_at")
  private LocalDateTime endedAt;

  @Column(name = "current_marker", insertable = false, updatable = false)
  private Byte currentMarker;
}

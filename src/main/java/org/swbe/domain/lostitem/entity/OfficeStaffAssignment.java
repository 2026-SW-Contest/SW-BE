package org.swbe.domain.lostitem.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="office_staff_assignment") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class OfficeStaffAssignment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="assignment_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="office_id") private LostItemOffice office;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private AppUser user;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assigned_by") private AppUser assignedBy;
 @Column(name="assigned_at",nullable=false) private LocalDateTime assignedAt; @Column(name="ended_at") private LocalDateTime endedAt;
 @Column(name="active_marker",insertable=false,updatable=false) private Integer activeMarker;
}

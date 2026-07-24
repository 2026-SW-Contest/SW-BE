package org.swbe.domain.servicerequest.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="request_status_history") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class RequestStatusHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="request_status_history_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_request_id") private ServiceRequest serviceRequest;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="changed_by") private AppUser changedBy;
 @Column(name="actor_type",nullable=false,length=20) private String actorType="USER"; @Column(name="previous_status",length=30) private String previousStatus;
 @Column(name="new_status",nullable=false,length=30) private String newStatus; @Lob @Column(name="change_reason") private String changeReason;
 @Column(name="changed_at",nullable=false) private LocalDateTime changedAt;
}

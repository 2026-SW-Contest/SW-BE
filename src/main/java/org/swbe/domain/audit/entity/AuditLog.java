package org.swbe.domain.audit.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="audit_log") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="audit_log_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="actor_user_id") private AppUser actor;
 @Column(name="actor_type",nullable=false,length=20) private String actorType="USER";
 @Column(name="target_type",nullable=false,length=50) private String targetType; @Column(name="target_id") private Long targetId;
 @Column(name="target_display_value",length=255) private String targetDisplayValue;
 @Column(name="action_type",nullable=false,length=50) private String actionType;
 @Column(name="before_value",columnDefinition="json") private String beforeValue; @Column(name="after_value",columnDefinition="json") private String afterValue;
 @Lob @Column(name="action_reason") private String actionReason; @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
}

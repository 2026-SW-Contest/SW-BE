package org.swbe.domain.lostitem.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="claim_status_history") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ClaimStatusHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="claim_status_history_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_claim_id") private ItemClaim itemClaim;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="changed_by") private AppUser changedBy;
 @Column(name="actor_type",nullable=false,length=20) private String actorType="USER"; @Column(name="previous_status",length=40) private String previousStatus;
 @Column(name="new_status",nullable=false,length=40) private String newStatus; @Lob @Column(name="change_reason") private String changeReason;
 @Column(name="changed_at",nullable=false) private LocalDateTime changedAt;
}

package org.swbe.domain.lostitem.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="item_claim") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ItemClaim {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="item_claim_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="stored_item_id") private StoredItem storedItem;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="claimant_user_id") private AppUser claimantUser;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="temporary_claimant_id") private TemporaryClaimant temporaryClaimant;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private AppUser reviewedBy;
 @Column(name="request_method",nullable=false,length=30) private String requestMethod;
 @Column(name="claim_status",nullable=false,length=40) private String claimStatus="PENDING";
 @Lob @Column(name="ownership_description") private String ownershipDescription; @Column(name="expected_lost_location",length=255) private String expectedLostLocation;
 @Column(name="expected_lost_at") private LocalDateTime expectedLostAt; @Lob @Column(name="rejection_reason") private String rejectionReason;
 @Lob @Column(name="closure_reason") private String closureReason; @Column(name="approved_at") private LocalDateTime approvedAt;
 @Column(name="collected_at") private LocalDateTime collectedAt; @Column(name="canceled_at") private LocalDateTime canceledAt; @Version private long version;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}

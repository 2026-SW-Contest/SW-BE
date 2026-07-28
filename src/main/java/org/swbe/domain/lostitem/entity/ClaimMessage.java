package org.swbe.domain.lostitem.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="claim_message") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ClaimMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="claim_message_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_claim_id") private ItemClaim itemClaim;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="author_user_id") private AppUser author;
 @Column(name="message_type",nullable=false,length=30) private String messageType; @Lob @Column(nullable=false) private String content;
 @Column(name="is_internal",nullable=false) private boolean internal; @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
}

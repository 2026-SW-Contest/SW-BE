package org.swbe.domain.lostitem.entity;
import jakarta.persistence.*; import lombok.*; import org.swbe.domain.file.entity.FileResource;
@Entity @Table(name="item_claim_attachment") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ItemClaimAttachment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="attachment_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_claim_id") private ItemClaim itemClaim;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="file_id") private FileResource file;
}

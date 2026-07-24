package org.swbe.domain.lostitem.entity;
import jakarta.persistence.*; import lombok.*; import org.swbe.domain.file.entity.FileResource;
@Entity @Table(name="claim_message_attachment") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ClaimMessageAttachment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="attachment_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="claim_message_id") private ClaimMessage claimMessage;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="file_id") private FileResource file;
}

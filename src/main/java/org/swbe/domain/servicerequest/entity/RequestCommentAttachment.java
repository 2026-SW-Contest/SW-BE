package org.swbe.domain.servicerequest.entity;
import jakarta.persistence.*; import lombok.*; import org.swbe.domain.file.entity.FileResource;
@Entity @Table(name="request_comment_attachment") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class RequestCommentAttachment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="attachment_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="request_comment_id") private RequestComment requestComment;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="file_id") private FileResource file;
}

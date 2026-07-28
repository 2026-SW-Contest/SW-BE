package org.swbe.domain.servicerequest.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="request_comment") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class RequestComment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="request_comment_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_request_id") private ServiceRequest serviceRequest;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="author_user_id") private AppUser author;
 @Column(name="comment_type",nullable=false,length=30) private String commentType; @Lob @Column(nullable=false) private String content;
 @Column(name="is_internal",nullable=false) private boolean internal; @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
}

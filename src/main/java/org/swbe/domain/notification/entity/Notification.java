package org.swbe.domain.notification.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="notification") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Notification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="notification_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="recipient_user_id") private AppUser recipient;
 @Column(name="notification_type",nullable=false,length=50) private String notificationType;
 @Column(name="reference_type",length=50) private String referenceType; @Column(name="reference_id") private Long referenceId;
 @Column(nullable=false,length=200) private String title; @Lob @Column(nullable=false) private String content;
 @Column(name="delivery_channel",nullable=false,length=30) private String deliveryChannel="WEB";
 @Column(name="delivery_status",nullable=false,length=30) private String deliveryStatus="PENDING";
 @Column(name="event_key",unique=true,length=150) private String eventKey; @Column(name="read_at") private LocalDateTime readAt;
 @Column(name="sent_at") private LocalDateTime sentAt; @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
}

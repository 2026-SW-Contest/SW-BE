package org.swbe.domain.servicerequest.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.campus.entity.Location; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="service_request") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ServiceRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="service_request_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="request_category_id") private RequestCategory requestCategory;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="location_id") private Location location;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="requester_user_id") private AppUser requester;
 @Column(name="receipt_number",nullable=false,unique=true,length=50) private String receiptNumber;
 @Column(nullable=false,length=200) private String title; @Lob @Column(nullable=false) private String description;
 @Column(name="equipment_name",length=150) private String equipmentName; @Column(nullable=false,length=20) private String visibility="PRIVATE";
 @Column(name="request_status",nullable=false,length=30) private String requestStatus="RECEIVED"; @Version private long version;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
 @Column(name="completed_at") private LocalDateTime completedAt;
}

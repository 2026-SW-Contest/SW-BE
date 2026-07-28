package org.swbe.domain.lostitem.entity;
import jakarta.persistence.*; import lombok.*; import java.time.*; import org.swbe.domain.campus.entity.Location; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="stored_item") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class StoredItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="stored_item_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="office_id") private LostItemOffice office;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="found_location_id") private Location foundLocation;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="registered_by") private AppUser registeredBy;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_category_id") private ItemCategory itemCategory;
 @Column(name="item_name",nullable=false,length=150) private String itemName;
 @Column(name="public_status",nullable=false,length=30) private String publicStatus="STORED";
 @Lob @Column(name="public_description") private String publicDescription; @Lob @Column(name="private_description") private String privateDescription;
 @Column(name="found_date",nullable=false) private LocalDate foundDate; @Column(name="found_time") private LocalTime foundTime;
 @Column(name="found_time_unknown",nullable=false) private boolean foundTimeUnknown; @Column(name="received_at",nullable=false) private LocalDateTime receivedAt;
 @Column(name="storage_position",length=255) private String storagePosition; @Column(name="storage_deadline",nullable=false) private LocalDate storageDeadline;
 @Column(name="collected_at") private LocalDateTime collectedAt; @Column(name="storage_closed_at") private LocalDateTime storageClosedAt;
 @Column(name="storage_close_reason",length=255) private String storageCloseReason; @Version private long version;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}

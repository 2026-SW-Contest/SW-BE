package org.swbe.domain.file.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="file_resource") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class FileResource {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="file_id") private Long id;
 @Column(name="storage_provider",nullable=false,length=30) private String storageProvider;
 @Column(name="storage_key",nullable=false,length=500) private String storageKey;
 @Column(name="original_filename",nullable=false,length=255) private String originalFilename;
 @Column(name="mime_type",nullable=false,length=100) private String mimeType;
 @Column(name="file_size",nullable=false) private long fileSize; @Column(length=128) private String checksum;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="uploaded_by") private AppUser uploadedBy;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @Column(name="deleted_at") private LocalDateTime deletedAt;
}

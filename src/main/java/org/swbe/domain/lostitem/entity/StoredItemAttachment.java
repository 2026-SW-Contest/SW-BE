package org.swbe.domain.lostitem.entity;
import jakarta.persistence.*; import lombok.*; import org.swbe.domain.file.entity.FileResource;
@Entity @Table(name="stored_item_attachment") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class StoredItemAttachment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="attachment_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="stored_item_id") private StoredItem storedItem;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="file_id") private FileResource file;
 @Column(name="is_primary",nullable=false) private boolean primary; @Column(name="display_order",nullable=false) private int displayOrder;
}

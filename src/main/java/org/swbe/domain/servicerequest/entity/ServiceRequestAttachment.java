package org.swbe.domain.servicerequest.entity;
import jakarta.persistence.*; import lombok.*; import org.swbe.domain.file.entity.FileResource;
@Entity @Table(name="service_request_attachment") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ServiceRequestAttachment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="attachment_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_request_id") private ServiceRequest serviceRequest;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="file_id") private FileResource file;
}

package org.swbe.domain.servicerequest.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="request_category") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class RequestCategory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="request_category_id") private Long id;
 @Column(name="category_name",nullable=false,unique=true,length=100) private String name;
 @Column(name="category_type",nullable=false,length=30) private String type; @Column(name="is_active",nullable=false) private boolean active=true;
}

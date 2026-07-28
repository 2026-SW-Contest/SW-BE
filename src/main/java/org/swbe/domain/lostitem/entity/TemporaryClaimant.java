package org.swbe.domain.lostitem.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime; import org.swbe.domain.campus.entity.Department; import org.swbe.domain.user.entity.AppUser;
@Entity @Table(name="temporary_claimant") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class TemporaryClaimant {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="temporary_claimant_id") private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="department_id") private Department department;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="linked_user_id") private AppUser linkedUser;
 @Column(nullable=false,length=100) private String name; @Column(name="student_number",nullable=false,unique=true,length=30) private String studentNumber;
 @Column(name="linked_at") private LocalDateTime linkedAt; @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}

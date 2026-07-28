package org.swbe.domain.lostitem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.swbe.domain.user.entity.AppUser;

@Entity
@Table(name = "claim_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "claim_message_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_claim_id")
  private ItemClaim itemClaim;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_user_id")
  private AppUser author;

  @Column(name = "message_type", nullable = false, length = 30)
  private String messageType;

  @Lob
  @Column(nullable = false)
  private String content;

  @Column(name = "is_internal", nullable = false)
  private boolean internal;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}

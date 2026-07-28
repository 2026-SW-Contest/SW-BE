package org.swbe.domain.lostitem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.swbe.domain.file.entity.FileResource;

@Entity
@Table(name = "claim_message_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimMessageAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attachment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_message_id")
  private ClaimMessage claimMessage;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "file_id")
  private FileResource file;
}

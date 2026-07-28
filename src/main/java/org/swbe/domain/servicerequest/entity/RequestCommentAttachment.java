package org.swbe.domain.servicerequest.entity;

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
@Table(name = "request_comment_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestCommentAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attachment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_comment_id")
  private RequestComment requestComment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "file_id")
  private FileResource file;
}

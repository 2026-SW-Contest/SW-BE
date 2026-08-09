package org.swbe.domain.facilityrequest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.swbe.domain.file.entity.FileResource;

@Entity
@Table(name = "facility_request_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FacilityRequestAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attachment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "facility_request_id")
  private FacilityRequest facilityRequest;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "file_id")
  private FileResource file;

  private FacilityRequestAttachment(
      FacilityRequest facilityRequest,
      FileResource file
  ) {
    this.facilityRequest = Objects.requireNonNull(facilityRequest);
    this.file = Objects.requireNonNull(file);
  }

  public static FacilityRequestAttachment attach(
      FacilityRequest facilityRequest,
      FileResource file
  ) {
    return new FacilityRequestAttachment(facilityRequest, file);
  }
}

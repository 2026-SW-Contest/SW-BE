package org.swbe.domain.facilityrequest.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacilityRequestStatus {

  WAITING("대기"),
  IN_PROGRESS("진행중"),
  COMPLETED("완료"),
  REJECTED("반려"),
  CANCELED("취소");

  private final String displayName;
}

package org.swbe.domain.facilityrequest.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FacilityRequestStatus {

  RECEIVED("접수"),
  ASSIGNED("담당자 배정"),
  CHECKING("검토중"),
  ADDITIONAL_INFO_REQUESTED("추가 정보 요청"),
  SCHEDULED("처리 예정"),
  IN_PROGRESS("진행중"),
  COMPLETED("답변완료"),
  UNAVAILABLE("처리 불가"),
  REJECTED("반려"),
  CANCELED("취소");

  private final String displayName;
}

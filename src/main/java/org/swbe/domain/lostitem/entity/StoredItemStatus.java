package org.swbe.domain.lostitem.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoredItemStatus {

  STORED("보관중"),
  IN_PROGRESS("진행중"),
  COMPLETED("해결완료");

  private final String displayName;
}

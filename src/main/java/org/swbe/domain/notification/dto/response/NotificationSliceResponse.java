package org.swbe.domain.notification.dto.response;

import java.util.List;

public record NotificationSliceResponse(
    List<NotificationListItemResponse> content,
    String nextCursor,
    boolean hasNext
) {

  public NotificationSliceResponse {
    content = List.copyOf(content);
  }
}

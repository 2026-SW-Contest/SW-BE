package org.swbe.domain.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.swbe.domain.notification.cursor.NotificationCursor;
import org.swbe.domain.notification.cursor.NotificationCursorCodec;
import org.swbe.domain.notification.dto.response.NotificationListItemResponse;
import org.swbe.domain.notification.dto.response.NotificationListResponse;
import org.swbe.domain.notification.dto.response.NotificationSliceResponse;
import org.swbe.domain.notification.entity.Notification;
import org.swbe.domain.notification.exception.NotificationErrorCode;
import org.swbe.domain.notification.repository.NotificationRepository;
import org.swbe.global.error.BusinessException;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationCursorCodec cursorCodec;
  private final Clock clock;

  @Transactional(readOnly = true)
  public NotificationListResponse getNotifications(
      Long recipientUserId,
      String encodedCursor,
      int size
  ) {
    NotificationCursor cursor = encodedCursor == null
        ? null
        : cursorCodec.decode(encodedCursor);
    List<Notification> matches = notificationRepository.findAllByCursor(
        recipientUserId,
        cursor == null ? null : cursor.createdAt(),
        cursor == null ? null : cursor.id(),
        PageRequest.of(0, size + 1)
    );
    boolean hasNext = matches.size() > size;
    List<Notification> content = hasNext
        ? matches.subList(0, size)
        : matches;

    return new NotificationListResponse(new NotificationSliceResponse(
        content.stream().map(this::toResponse).toList(),
        nextCursor(content, hasNext),
        hasNext
    ));
  }

  @Transactional
  public void read(Long notificationId, Long recipientUserId) {
    Notification notification = notificationRepository
        .findByIdAndRecipient_Id(notificationId, recipientUserId)
        .orElseThrow(() -> new BusinessException(
            NotificationErrorCode.NOT_FOUND
        ));
    notification.markAsRead(LocalDateTime.now(clock));
  }

  private NotificationListItemResponse toResponse(
      Notification notification
  ) {
    return new NotificationListItemResponse(
        notification.getId(),
        notification.getNotificationType(),
        notification.getTitle(),
        notification.getContent(),
        notification.getReferenceType(),
        notification.getReferenceId(),
        notification.getReadAt() != null,
        notification.getReadAt(),
        notification.getCreatedAt()
    );
  }

  private String nextCursor(
      List<Notification> content,
      boolean hasNext
  ) {
    if (!hasNext) {
      return null;
    }
    Notification last = content.getLast();
    return cursorCodec.encode(last.getCreatedAt(), last.getId());
  }
}

package org.swbe.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.swbe.domain.notification.cursor.NotificationCursorCodec;
import org.swbe.domain.notification.entity.Notification;
import org.swbe.domain.notification.exception.NotificationErrorCode;
import org.swbe.domain.notification.repository.NotificationRepository;
import org.swbe.domain.user.entity.AppUser;
import org.swbe.global.error.BusinessException;

class NotificationServiceTest {

  private static final Long USER_ID = 7L;
  private static final LocalDateTime NOW = LocalDateTime.of(
      2026, 8, 12, 15, 0
  );

  private NotificationRepository notificationRepository;
  private NotificationCursorCodec cursorCodec;
  private NotificationService service;

  @BeforeEach
  void setUp() {
    notificationRepository = mock(NotificationRepository.class);
    cursorCodec = new NotificationCursorCodec();
    Clock clock = Clock.fixed(
        Instant.parse("2026-08-12T15:00:00Z"),
        ZoneOffset.UTC
    );
    service = new NotificationService(
        notificationRepository,
        cursorCodec,
        clock
    );
  }

  @Test
  void returnsRecipientNotificationsWithCursorAndReadStatus() {
    Notification first = notification(
        10L,
        LocalDateTime.of(2026, 8, 12, 14, 30)
    );
    Notification second = notification(
        9L,
        LocalDateTime.of(2026, 8, 12, 14, 0)
    );
    first.markAsRead(LocalDateTime.of(2026, 8, 12, 14, 40));
    when(notificationRepository.findAllByCursor(
        eq(USER_ID),
        eq(null),
        eq(null),
        any(Pageable.class)
    )).thenReturn(List.of(first, second));

    var response = service.getNotifications(USER_ID, null, 1);

    assertThat(response.data().content()).singleElement()
        .satisfies(item -> {
          assertThat(item.notificationId()).isEqualTo(10L);
          assertThat(item.notificationType())
              .isEqualTo("ITEM_CLAIM_DECIDED");
          assertThat(item.referenceType()).isEqualTo("STORED_ITEM");
          assertThat(item.referenceId()).isEqualTo(25L);
          assertThat(item.read()).isTrue();
          assertThat(item.readAt()).isEqualTo(
              LocalDateTime.of(2026, 8, 12, 14, 40)
          );
        });
    assertThat(response.data().hasNext()).isTrue();
    assertThat(response.data().nextCursor()).isNotBlank();
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(
        Pageable.class
    );
    verify(notificationRepository).findAllByCursor(
        eq(USER_ID),
        eq(null),
        eq(null),
        pageableCaptor.capture()
    );
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
  }

  @Test
  void passesDecodedCursorAndReturnsNoNextCursorForLastSlice() {
    LocalDateTime cursorCreatedAt = LocalDateTime.of(
        2026, 8, 12, 14, 30
    );
    String encodedCursor = cursorCodec.encode(cursorCreatedAt, 10L);
    when(notificationRepository.findAllByCursor(
        eq(USER_ID),
        eq(cursorCreatedAt),
        eq(10L),
        any(Pageable.class)
    )).thenReturn(List.of());

    var response = service.getNotifications(
        USER_ID,
        encodedCursor,
        20
    );

    assertThat(response.data().content()).isEmpty();
    assertThat(response.data().hasNext()).isFalse();
    assertThat(response.data().nextCursor()).isNull();
  }

  @Test
  void marksOwnedNotificationAsRead() {
    Notification notification = notification(10L, NOW.minusMinutes(30));
    when(notificationRepository.findByIdAndRecipient_Id(10L, USER_ID))
        .thenReturn(Optional.of(notification));

    service.read(10L, USER_ID);

    assertThat(notification.getReadAt()).isEqualTo(NOW);
  }

  @Test
  void readingAlreadyReadNotificationKeepsOriginalReadTime() {
    Notification notification = notification(10L, NOW.minusMinutes(30));
    LocalDateTime originalReadAt = NOW.minusMinutes(10);
    notification.markAsRead(originalReadAt);
    when(notificationRepository.findByIdAndRecipient_Id(10L, USER_ID))
        .thenReturn(Optional.of(notification));

    service.read(10L, USER_ID);

    assertThat(notification.getReadAt()).isEqualTo(originalReadAt);
  }

  @Test
  void missingOrOtherRecipientsNotificationReturnsNotFound() {
    when(notificationRepository.findByIdAndRecipient_Id(99L, USER_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.read(99L, USER_ID))
        .isInstanceOf(BusinessException.class)
        .satisfies(exception -> assertThat(
            ((BusinessException) exception).getErrorCode()
        ).isEqualTo(NotificationErrorCode.NOT_FOUND));
  }

  @Test
  void returnsUnreadCountForCurrentRecipient() {
    when(notificationRepository
        .countByRecipient_IdAndReadAtIsNull(USER_ID))
        .thenReturn(3L);

    var response = service.getUnreadCount(USER_ID);

    assertThat(response.data().unreadCount()).isEqualTo(3L);
  }

  @Test
  void returnsZeroWhenRecipientHasNoUnreadNotifications() {
    when(notificationRepository
        .countByRecipient_IdAndReadAtIsNull(USER_ID))
        .thenReturn(0L);

    var response = service.getUnreadCount(USER_ID);

    assertThat(response.data().unreadCount()).isZero();
  }

  @Test
  void marksAllUnreadNotificationsAtSameCurrentTime() {
    when(notificationRepository.markAllAsRead(USER_ID, NOW))
        .thenReturn(3);

    service.readAll(USER_ID);

    verify(notificationRepository).markAllAsRead(USER_ID, NOW);
  }

  @Test
  void readAllSucceedsWhenThereAreNoUnreadNotifications() {
    when(notificationRepository.markAllAsRead(USER_ID, NOW))
        .thenReturn(0);

    service.readAll(USER_ID);

    verify(notificationRepository).markAllAsRead(USER_ID, NOW);
  }

  private Notification notification(
      Long id,
      LocalDateTime createdAt
  ) {
    Notification notification = Notification.createItemClaimDecision(
        mock(AppUser.class),
        25L,
        "소유자 확인 요청이 승인되었습니다.",
        "학생증과 물품 특징을 확인했습니다.",
        createdAt
    );
    ReflectionTestUtils.setField(notification, "id", id);
    return notification;
  }
}

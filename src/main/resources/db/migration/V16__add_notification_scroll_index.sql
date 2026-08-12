CREATE INDEX idx_notification_recipient_scroll
    ON notification
        (recipient_user_id, created_at DESC, notification_id DESC);

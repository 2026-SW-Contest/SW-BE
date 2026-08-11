ALTER TABLE stored_item
    DROP CHECK ck_stored_item_collected,
    DROP CHECK ck_stored_item_closed,
    DROP CHECK ck_stored_item_status;

UPDATE stored_item
SET public_status = CASE public_status
    WHEN 'STORED' THEN 'STORED'
    WHEN 'CHECKING' THEN 'IN_PROGRESS'
    WHEN 'PICKUP_SCHEDULED' THEN 'IN_PROGRESS'
    WHEN 'COLLECTED' THEN 'COMPLETED'
    WHEN 'STORAGE_CLOSED' THEN 'COMPLETED'
    ELSE public_status
END;

UPDATE item_status_history
SET previous_status = CASE previous_status
    WHEN 'STORED' THEN 'STORED'
    WHEN 'CHECKING' THEN 'IN_PROGRESS'
    WHEN 'PICKUP_SCHEDULED' THEN 'IN_PROGRESS'
    WHEN 'COLLECTED' THEN 'COMPLETED'
    WHEN 'STORAGE_CLOSED' THEN 'COMPLETED'
    ELSE previous_status
END,
    new_status = CASE new_status
    WHEN 'STORED' THEN 'STORED'
    WHEN 'CHECKING' THEN 'IN_PROGRESS'
    WHEN 'PICKUP_SCHEDULED' THEN 'IN_PROGRESS'
    WHEN 'COLLECTED' THEN 'COMPLETED'
    WHEN 'STORAGE_CLOSED' THEN 'COMPLETED'
    ELSE new_status
END;

ALTER TABLE stored_item
    ADD CONSTRAINT ck_stored_item_status
        CHECK (public_status IN ('STORED', 'IN_PROGRESS', 'COMPLETED'));

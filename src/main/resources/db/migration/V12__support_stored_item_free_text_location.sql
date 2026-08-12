ALTER TABLE stored_item
    ADD COLUMN found_location_text VARCHAR(255) NULL AFTER found_location_id,
    MODIFY COLUMN storage_deadline DATE NULL;

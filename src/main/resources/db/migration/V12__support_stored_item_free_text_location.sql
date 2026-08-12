ALTER TABLE stored_item
    ADD COLUMN found_location_text VARCHAR(255) NULL AFTER found_location_id,
    MODIFY COLUMN storage_deadline DATE NULL;

ALTER TABLE stored_item
    ADD CONSTRAINT ck_stored_item_found_location
        CHECK (found_location_id IS NULL OR found_location_text IS NULL);

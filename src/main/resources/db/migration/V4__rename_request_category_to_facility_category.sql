ALTER TABLE service_request
    DROP FOREIGN KEY fk_service_request_category;

RENAME TABLE request_category TO facility_category;

ALTER TABLE facility_category
    RENAME COLUMN request_category_id TO facility_category_id,
    RENAME INDEX uk_request_category_name TO uk_facility_category_name;

ALTER TABLE service_request
    RENAME COLUMN request_category_id TO facility_category_id,
    ADD CONSTRAINT fk_service_request_facility_category
        FOREIGN KEY (facility_category_id)
            REFERENCES facility_category (facility_category_id)
            ON DELETE RESTRICT;

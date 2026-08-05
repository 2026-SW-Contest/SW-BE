ALTER TABLE request_assignment
    DROP FOREIGN KEY fk_request_assignment_request;

ALTER TABLE request_comment
    DROP FOREIGN KEY fk_request_comment_request;

ALTER TABLE request_status_history
    DROP FOREIGN KEY fk_request_status_history_request;

ALTER TABLE service_request_attachment
    DROP FOREIGN KEY fk_service_request_attachment_request,
    DROP FOREIGN KEY fk_service_request_attachment_file;

ALTER TABLE service_request
    DROP FOREIGN KEY fk_service_request_facility_category,
    DROP FOREIGN KEY fk_service_request_location,
    DROP FOREIGN KEY fk_service_request_requester,
    DROP CHECK ck_service_request_visibility,
    DROP CHECK ck_service_request_status,
    DROP CHECK ck_service_request_completed,
    DROP CHECK ck_service_request_version;

RENAME TABLE service_request TO facility_request;

ALTER TABLE facility_request
    RENAME COLUMN service_request_id TO facility_request_id,
    RENAME INDEX uk_service_request_receipt TO uk_facility_request_receipt,
    RENAME INDEX idx_service_request_status TO idx_facility_request_status,
    RENAME INDEX idx_service_request_requester TO idx_facility_request_requester,
    ADD CONSTRAINT fk_facility_request_facility_category
        FOREIGN KEY (facility_category_id)
            REFERENCES facility_category (facility_category_id)
            ON DELETE RESTRICT,
    ADD CONSTRAINT fk_facility_request_location
        FOREIGN KEY (location_id)
            REFERENCES location (location_id)
            ON DELETE RESTRICT,
    ADD CONSTRAINT fk_facility_request_requester
        FOREIGN KEY (requester_user_id)
            REFERENCES app_user (user_id)
            ON DELETE RESTRICT,
    ADD CONSTRAINT ck_facility_request_visibility
        CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    ADD CONSTRAINT ck_facility_request_status
        CHECK (request_status IN ('RECEIVED', 'ASSIGNED', 'CHECKING',
                                  'ADDITIONAL_INFO_REQUESTED', 'SCHEDULED',
                                  'IN_PROGRESS', 'COMPLETED', 'UNAVAILABLE',
                                  'REJECTED', 'CANCELED')),
    ADD CONSTRAINT ck_facility_request_completed
        CHECK (request_status <> 'COMPLETED' OR completed_at IS NOT NULL),
    ADD CONSTRAINT ck_facility_request_version
        CHECK (version >= 0);

ALTER TABLE request_assignment
    RENAME COLUMN service_request_id TO facility_request_id,
    ADD CONSTRAINT fk_request_assignment_facility_request
        FOREIGN KEY (facility_request_id)
            REFERENCES facility_request (facility_request_id)
            ON DELETE RESTRICT;

ALTER TABLE request_comment
    RENAME COLUMN service_request_id TO facility_request_id,
    ADD CONSTRAINT fk_request_comment_facility_request
        FOREIGN KEY (facility_request_id)
            REFERENCES facility_request (facility_request_id)
            ON DELETE CASCADE;

ALTER TABLE request_status_history
    RENAME COLUMN service_request_id TO facility_request_id,
    ADD CONSTRAINT fk_request_status_history_facility_request
        FOREIGN KEY (facility_request_id)
            REFERENCES facility_request (facility_request_id)
            ON DELETE RESTRICT;

RENAME TABLE service_request_attachment TO facility_request_attachment;

ALTER TABLE facility_request_attachment
    RENAME COLUMN service_request_id TO facility_request_id,
    RENAME INDEX uk_service_request_attachment TO uk_facility_request_attachment,
    ADD CONSTRAINT fk_facility_request_attachment_request
        FOREIGN KEY (facility_request_id)
            REFERENCES facility_request (facility_request_id)
            ON DELETE CASCADE,
    ADD CONSTRAINT fk_facility_request_attachment_file
        FOREIGN KEY (file_id)
            REFERENCES file_resource (file_id)
            ON DELETE RESTRICT;

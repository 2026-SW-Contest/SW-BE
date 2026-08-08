ALTER TABLE facility_request
    DROP INDEX uk_facility_request_receipt,
    DROP CHECK ck_facility_request_visibility,
    DROP COLUMN receipt_number,
    DROP COLUMN equipment_name,
    DROP COLUMN visibility;

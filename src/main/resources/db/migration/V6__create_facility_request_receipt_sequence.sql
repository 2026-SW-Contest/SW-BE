CREATE TABLE facility_request_receipt_sequence
(
    receipt_date  DATE   NOT NULL,
    current_value BIGINT NOT NULL,
    CONSTRAINT pk_facility_request_receipt_sequence
        PRIMARY KEY (receipt_date),
    CONSTRAINT ck_facility_request_receipt_sequence_value
        CHECK (current_value > 0)
) ENGINE=InnoDB;

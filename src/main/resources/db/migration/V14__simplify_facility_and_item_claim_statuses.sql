ALTER TABLE facility_request
    DROP CHECK ck_facility_request_status;

UPDATE facility_request
SET request_status = CASE request_status
    WHEN 'RECEIVED' THEN 'WAITING'
    WHEN 'ASSIGNED' THEN 'IN_PROGRESS'
    WHEN 'CHECKING' THEN 'IN_PROGRESS'
    WHEN 'ADDITIONAL_INFO_REQUESTED' THEN 'IN_PROGRESS'
    WHEN 'SCHEDULED' THEN 'IN_PROGRESS'
    WHEN 'UNAVAILABLE' THEN 'REJECTED'
    ELSE request_status
END;

UPDATE request_status_history
SET previous_status = CASE previous_status
        WHEN 'RECEIVED' THEN 'WAITING'
        WHEN 'ASSIGNED' THEN 'IN_PROGRESS'
        WHEN 'CHECKING' THEN 'IN_PROGRESS'
        WHEN 'ADDITIONAL_INFO_REQUESTED' THEN 'IN_PROGRESS'
        WHEN 'SCHEDULED' THEN 'IN_PROGRESS'
        WHEN 'UNAVAILABLE' THEN 'REJECTED'
        ELSE previous_status
    END,
    new_status = CASE new_status
        WHEN 'RECEIVED' THEN 'WAITING'
        WHEN 'ASSIGNED' THEN 'IN_PROGRESS'
        WHEN 'CHECKING' THEN 'IN_PROGRESS'
        WHEN 'ADDITIONAL_INFO_REQUESTED' THEN 'IN_PROGRESS'
        WHEN 'SCHEDULED' THEN 'IN_PROGRESS'
        WHEN 'UNAVAILABLE' THEN 'REJECTED'
        ELSE new_status
    END;

ALTER TABLE facility_request
    MODIFY COLUMN request_status VARCHAR(30) NOT NULL
        DEFAULT 'WAITING',
    ADD CONSTRAINT ck_facility_request_status CHECK (
        request_status IN (
            'WAITING',
            'IN_PROGRESS',
            'COMPLETED',
            'REJECTED',
            'CANCELED'
        )
    );

ALTER TABLE item_claim
    DROP CHECK ck_item_claim_status,
    DROP CHECK ck_item_claim_approved,
    DROP CHECK ck_item_claim_collected,
    DROP CHECK ck_item_claim_canceled;

UPDATE item_claim
SET approved_at = CASE
        WHEN claim_status = 'COLLECTED'
            THEN COALESCE(approved_at, collected_at, updated_at)
        ELSE approved_at
    END,
    claim_status = CASE claim_status
        WHEN 'PENDING' THEN 'WAITING'
        WHEN 'ADDITIONAL_INFO_REQUESTED' THEN 'IN_PROGRESS'
        WHEN 'COLLECTED' THEN 'APPROVED'
        WHEN 'CANCELED' THEN 'REJECTED'
        WHEN 'CLOSED_BY_STORAGE_END' THEN 'REJECTED'
        ELSE claim_status
    END;

UPDATE claim_status_history
SET previous_status = CASE previous_status
        WHEN 'PENDING' THEN 'WAITING'
        WHEN 'ADDITIONAL_INFO_REQUESTED' THEN 'IN_PROGRESS'
        WHEN 'COLLECTED' THEN 'APPROVED'
        WHEN 'CANCELED' THEN 'REJECTED'
        WHEN 'CLOSED_BY_STORAGE_END' THEN 'REJECTED'
        ELSE previous_status
    END,
    new_status = CASE new_status
        WHEN 'PENDING' THEN 'WAITING'
        WHEN 'ADDITIONAL_INFO_REQUESTED' THEN 'IN_PROGRESS'
        WHEN 'COLLECTED' THEN 'APPROVED'
        WHEN 'CANCELED' THEN 'REJECTED'
        WHEN 'CLOSED_BY_STORAGE_END' THEN 'REJECTED'
        ELSE new_status
    END;

ALTER TABLE item_claim
    MODIFY COLUMN claim_status VARCHAR(40) NOT NULL
        DEFAULT 'WAITING',
    ADD CONSTRAINT ck_item_claim_status CHECK (
        claim_status IN (
            'WAITING',
            'IN_PROGRESS',
            'APPROVED',
            'REJECTED',
            'CLOSED_BY_OTHER_COLLECTION'
        )
    ),
    ADD CONSTRAINT ck_item_claim_approved CHECK (
        claim_status <> 'APPROVED' OR approved_at IS NOT NULL
    );

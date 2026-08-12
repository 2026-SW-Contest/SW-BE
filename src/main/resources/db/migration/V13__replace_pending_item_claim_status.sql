ALTER TABLE item_claim
    DROP CHECK ck_item_claim_status;

UPDATE item_claim
SET claim_status = 'IN_PROGRESS'
WHERE claim_status = 'PENDING';

UPDATE claim_status_history
SET previous_status = CASE previous_status
        WHEN 'PENDING' THEN 'IN_PROGRESS'
        ELSE previous_status
    END,
    new_status = CASE new_status
        WHEN 'PENDING' THEN 'IN_PROGRESS'
        ELSE new_status
    END;

ALTER TABLE item_claim
    MODIFY COLUMN claim_status VARCHAR(40) NOT NULL
        DEFAULT 'IN_PROGRESS',
    ADD CONSTRAINT ck_item_claim_status CHECK (
        claim_status IN (
            'IN_PROGRESS',
            'ADDITIONAL_INFO_REQUESTED',
            'APPROVED',
            'REJECTED',
            'CANCELED',
            'COLLECTED',
            'CLOSED_BY_OTHER_COLLECTION',
            'CLOSED_BY_STORAGE_END'
        )
    );

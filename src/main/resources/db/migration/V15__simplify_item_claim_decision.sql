ALTER TABLE item_claim
    DROP CHECK ck_item_claim_status,
    DROP CHECK ck_item_claim_approved;

UPDATE item_claim
SET approved_at = CASE
        WHEN claim_status IN (
            'APPROVED',
            'REJECTED',
            'CLOSED_BY_OTHER_COLLECTION'
        ) THEN COALESCE(approved_at, updated_at)
        ELSE approved_at
    END,
    claim_status = CASE claim_status
        WHEN 'IN_PROGRESS' THEN 'WAITING'
        WHEN 'CLOSED_BY_OTHER_COLLECTION' THEN 'REJECTED'
        ELSE claim_status
    END
WHERE claim_status IN (
    'IN_PROGRESS',
    'APPROVED',
    'REJECTED',
    'CLOSED_BY_OTHER_COLLECTION'
);

UPDATE claim_status_history
SET previous_status = CASE previous_status
        WHEN 'IN_PROGRESS' THEN 'WAITING'
        WHEN 'CLOSED_BY_OTHER_COLLECTION' THEN 'REJECTED'
        ELSE previous_status
    END,
    new_status = CASE new_status
        WHEN 'IN_PROGRESS' THEN 'WAITING'
        WHEN 'CLOSED_BY_OTHER_COLLECTION' THEN 'REJECTED'
        ELSE new_status
    END;

ALTER TABLE item_claim
    CHANGE COLUMN rejection_reason decision_message TEXT NULL,
    CHANGE COLUMN approved_at decided_at DATETIME(6) NULL,
    ADD CONSTRAINT ck_item_claim_status CHECK (
        claim_status IN ('WAITING', 'APPROVED', 'REJECTED')
    ),
    ADD CONSTRAINT ck_item_claim_decided CHECK (
        claim_status = 'WAITING' OR decided_at IS NOT NULL
    );

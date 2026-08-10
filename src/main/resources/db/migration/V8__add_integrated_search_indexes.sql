CREATE INDEX idx_stored_item_search_scroll
    ON stored_item (created_at DESC, stored_item_id DESC);

CREATE INDEX idx_facility_request_search_scroll
    ON facility_request
        (created_at DESC, facility_request_id DESC);

CREATE TABLE recent_search
(
    recent_search_id   BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    keyword            VARCHAR(100) NOT NULL,
    normalized_keyword VARCHAR(100) NOT NULL,
    searched_at        DATETIME(6)  NOT NULL,
    CONSTRAINT pk_recent_search
        PRIMARY KEY (recent_search_id),
    CONSTRAINT uk_recent_search_user_keyword
        UNIQUE (user_id, normalized_keyword),
    CONSTRAINT fk_recent_search_user
        FOREIGN KEY (user_id)
            REFERENCES app_user (user_id)
            ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_recent_search_user_time
    ON recent_search
        (user_id, searched_at DESC, recent_search_id DESC);

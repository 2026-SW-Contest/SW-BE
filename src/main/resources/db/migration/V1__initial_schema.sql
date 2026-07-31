-- MySQL 8.0.16+
-- Flyway: V1__initial_schema.sql

CREATE TABLE department (
  department_id BIGINT NOT NULL AUTO_INCREMENT,
  department_name VARCHAR(100) NOT NULL,
  department_type VARCHAR(30) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_department PRIMARY KEY (department_id),
  CONSTRAINT uk_department_name UNIQUE (department_name)
) ENGINE=InnoDB;

CREATE TABLE campus (
  campus_id BIGINT NOT NULL AUTO_INCREMENT,
  campus_name VARCHAR(100) NOT NULL,
  address VARCHAR(255) NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_campus PRIMARY KEY (campus_id),
  CONSTRAINT uk_campus_name UNIQUE (campus_name)
) ENGINE=InnoDB;

CREATE TABLE building (
  building_id BIGINT NOT NULL AUTO_INCREMENT,
  campus_id BIGINT NOT NULL,
  building_name VARCHAR(100) NOT NULL,
  building_code VARCHAR(30) NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_building PRIMARY KEY (building_id),
  CONSTRAINT uk_building_name UNIQUE (campus_id, building_name),
  CONSTRAINT uk_building_code UNIQUE (building_code),
  CONSTRAINT fk_building_campus FOREIGN KEY (campus_id)
    REFERENCES campus (campus_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE location (
  location_id BIGINT NOT NULL AUTO_INCREMENT,
  building_id BIGINT NOT NULL,
  parent_location_id BIGINT NULL,
  location_name VARCHAR(100) NOT NULL,
  floor VARCHAR(30) NULL,
  room VARCHAR(50) NULL,
  description VARCHAR(255) NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_location PRIMARY KEY (location_id),
  CONSTRAINT fk_location_building FOREIGN KEY (building_id)
    REFERENCES building (building_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_location_parent FOREIGN KEY (parent_location_id)
    REFERENCES location (location_id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;
CREATE INDEX idx_location_building ON location (building_id);
CREATE INDEX idx_location_parent ON location (parent_location_id);

CREATE TABLE app_user (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  department_id BIGINT NULL,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NULL,
  name VARCHAR(100) NOT NULL,
  student_number VARCHAR(30) NULL,
  account_status VARCHAR(30) NOT NULL DEFAULT 'INVITED',
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_app_user PRIMARY KEY (user_id),
  CONSTRAINT uk_app_user_email UNIQUE (email),
  CONSTRAINT uk_app_user_student_number UNIQUE (student_number),
  CONSTRAINT fk_app_user_department FOREIGN KEY (department_id)
    REFERENCES department (department_id) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT ck_app_user_status CHECK (account_status IN ('INVITED','ACTIVE','SUSPENDED','WITHDRAWN','DEACTIVATED'))
) ENGINE=InnoDB;
CREATE INDEX idx_app_user_department ON app_user (department_id);

CREATE TABLE app_role (
  role_id BIGINT NOT NULL AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL,
  role_name VARCHAR(100) NOT NULL,
  CONSTRAINT pk_app_role PRIMARY KEY (role_id),
  CONSTRAINT uk_app_role_code UNIQUE (role_code),
  CONSTRAINT uk_app_role_name UNIQUE (role_name)
) ENGINE=InnoDB;

CREATE TABLE user_role (
  user_role_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  granted_by BIGINT NULL,
  granted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  revoked_at DATETIME(6) NULL,
  active_marker TINYINT GENERATED ALWAYS AS (CASE WHEN revoked_at IS NULL THEN 1 ELSE NULL END) STORED,
  CONSTRAINT pk_user_role PRIMARY KEY (user_role_id),
  CONSTRAINT uk_user_role_active UNIQUE (user_id, role_id, active_marker),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES app_role (role_id) ON DELETE RESTRICT,
  CONSTRAINT fk_user_role_granted_by FOREIGN KEY (granted_by) REFERENCES app_user (user_id) ON DELETE SET NULL,
  CONSTRAINT ck_user_role_period CHECK (revoked_at IS NULL OR revoked_at >= granted_at)
) ENGINE=InnoDB;

CREATE TABLE staff_invitation (
  invitation_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  created_by BIGINT NULL,
  token_hash VARCHAR(255) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6) NULL,
  revoked_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_staff_invitation PRIMARY KEY (invitation_id),
  CONSTRAINT uk_staff_invitation_token UNIQUE (token_hash),
  CONSTRAINT fk_staff_invitation_user FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_staff_invitation_created_by FOREIGN KEY (created_by) REFERENCES app_user (user_id) ON DELETE SET NULL,
  CONSTRAINT ck_staff_invitation_expiry CHECK (expires_at > created_at)
) ENGINE=InnoDB;

CREATE TABLE email_verification (
  verification_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  email VARCHAR(255) NOT NULL,
  code_hash VARCHAR(255) NOT NULL,
  purpose VARCHAR(30) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  expires_at DATETIME(6) NOT NULL,
  verified_at DATETIME(6) NULL,
  verification_token_hash CHAR(64) NULL,
  verification_token_expires_at DATETIME(6) NULL,
  consumed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_email_verification PRIMARY KEY (verification_id),
  CONSTRAINT uk_email_verification_token_hash UNIQUE (verification_token_hash),
  CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE SET NULL,
  CONSTRAINT ck_email_verification_attempt CHECK (attempt_count >= 0),
  CONSTRAINT ck_email_verification_expiry CHECK (expires_at > created_at),
  CONSTRAINT ck_email_verification_token_pair CHECK (
    (verification_token_hash IS NULL AND verification_token_expires_at IS NULL)
    OR (
      verification_token_hash IS NOT NULL
      AND verification_token_expires_at IS NOT NULL
      AND verified_at IS NOT NULL
      AND verification_token_expires_at > verified_at
    )
  ),
  CONSTRAINT ck_email_verification_consumed CHECK (
    consumed_at IS NULL
    OR (verified_at IS NOT NULL AND user_id IS NOT NULL)
  )
) ENGINE=InnoDB;
CREATE INDEX idx_email_verification_email ON email_verification (email, purpose);

CREATE TABLE lost_item_office (
  office_id BIGINT NOT NULL AUTO_INCREMENT,
  building_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  office_name VARCHAR(100) NOT NULL,
  operating_hours VARCHAR(255) NULL,
  guidance TEXT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  active_primary_marker TINYINT GENERATED ALWAYS AS
    (CASE WHEN is_primary = TRUE AND is_active = TRUE THEN 1 ELSE NULL END) STORED,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_lost_item_office PRIMARY KEY (office_id),
  CONSTRAINT uk_lost_item_office_name UNIQUE (building_id, office_name),
  CONSTRAINT uk_lost_item_office_primary UNIQUE (building_id, active_primary_marker),
  CONSTRAINT fk_office_building FOREIGN KEY (building_id) REFERENCES building (building_id) ON DELETE RESTRICT,
  CONSTRAINT fk_office_department FOREIGN KEY (department_id) REFERENCES department (department_id) ON DELETE RESTRICT,
  CONSTRAINT fk_office_location FOREIGN KEY (location_id) REFERENCES location (location_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE office_staff_assignment (
  assignment_id BIGINT NOT NULL AUTO_INCREMENT,
  office_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  assigned_by BIGINT NULL,
  assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ended_at DATETIME(6) NULL,
  active_marker TINYINT GENERATED ALWAYS AS (CASE WHEN ended_at IS NULL THEN 1 ELSE NULL END) STORED,
  CONSTRAINT pk_office_staff_assignment PRIMARY KEY (assignment_id),
  CONSTRAINT uk_office_staff_active UNIQUE (office_id, user_id, active_marker),
  CONSTRAINT fk_office_staff_office FOREIGN KEY (office_id) REFERENCES lost_item_office (office_id) ON DELETE RESTRICT,
  CONSTRAINT fk_office_staff_user FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_office_staff_assigned_by FOREIGN KEY (assigned_by) REFERENCES app_user (user_id) ON DELETE SET NULL,
  CONSTRAINT ck_office_staff_period CHECK (ended_at IS NULL OR ended_at >= assigned_at)
) ENGINE=InnoDB;

CREATE TABLE file_resource (
  file_id BIGINT NOT NULL AUTO_INCREMENT,
  storage_provider VARCHAR(30) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  checksum VARCHAR(128) NULL,
  uploaded_by BIGINT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  deleted_at DATETIME(6) NULL,
  CONSTRAINT pk_file_resource PRIMARY KEY (file_id),
  CONSTRAINT uk_file_resource_storage UNIQUE (storage_provider, storage_key),
  CONSTRAINT fk_file_resource_user FOREIGN KEY (uploaded_by) REFERENCES app_user (user_id) ON DELETE SET NULL,
  CONSTRAINT ck_file_resource_size CHECK (file_size >= 0)
) ENGINE=InnoDB;

CREATE TABLE item_category (
  item_category_id BIGINT NOT NULL AUTO_INCREMENT,
  category_name VARCHAR(100) NOT NULL,
  is_important_item BOOLEAN NOT NULL DEFAULT FALSE,
  default_storage_days INT NOT NULL DEFAULT 90,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_item_category PRIMARY KEY (item_category_id),
  CONSTRAINT uk_item_category_name UNIQUE (category_name),
  CONSTRAINT ck_item_category_days CHECK (default_storage_days > 0)
) ENGINE=InnoDB;

CREATE TABLE stored_item (
  stored_item_id BIGINT NOT NULL AUTO_INCREMENT,
  office_id BIGINT NOT NULL,
  found_location_id BIGINT NULL,
  registered_by BIGINT NOT NULL,
  item_category_id BIGINT NOT NULL,
  item_name VARCHAR(150) NOT NULL,
  public_status VARCHAR(30) NOT NULL DEFAULT 'STORED',
  public_description TEXT NULL,
  private_description TEXT NULL,
  found_date DATE NOT NULL,
  found_time TIME NULL,
  found_time_unknown BOOLEAN NOT NULL DEFAULT FALSE,
  received_at DATETIME(6) NOT NULL,
  storage_position VARCHAR(255) NULL,
  storage_deadline DATE NOT NULL,
  collected_at DATETIME(6) NULL,
  storage_closed_at DATETIME(6) NULL,
  storage_close_reason VARCHAR(255) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_stored_item PRIMARY KEY (stored_item_id),
  CONSTRAINT fk_stored_item_office FOREIGN KEY (office_id) REFERENCES lost_item_office (office_id) ON DELETE RESTRICT,
  CONSTRAINT fk_stored_item_location FOREIGN KEY (found_location_id) REFERENCES location (location_id) ON DELETE SET NULL,
  CONSTRAINT fk_stored_item_registered_by FOREIGN KEY (registered_by) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_stored_item_category FOREIGN KEY (item_category_id) REFERENCES item_category (item_category_id) ON DELETE RESTRICT,
  CONSTRAINT ck_stored_item_status CHECK (public_status IN ('STORED','CHECKING','PICKUP_SCHEDULED','COLLECTED','STORAGE_CLOSED')),
  CONSTRAINT ck_stored_item_found_time CHECK ((found_time_unknown = TRUE AND found_time IS NULL) OR found_time_unknown = FALSE),
  CONSTRAINT ck_stored_item_collected CHECK (public_status <> 'COLLECTED' OR collected_at IS NOT NULL),
  CONSTRAINT ck_stored_item_closed CHECK (public_status <> 'STORAGE_CLOSED' OR storage_closed_at IS NOT NULL),
  CONSTRAINT ck_stored_item_version CHECK (version >= 0)
) ENGINE=InnoDB;
CREATE INDEX idx_stored_item_status ON stored_item (office_id, public_status);
CREATE INDEX idx_stored_item_date ON stored_item (found_date);
CREATE INDEX idx_stored_item_deadline ON stored_item (storage_deadline);

CREATE TABLE temporary_claimant (
  temporary_claimant_id BIGINT NOT NULL AUTO_INCREMENT,
  department_id BIGINT NULL,
  linked_user_id BIGINT NULL,
  name VARCHAR(100) NOT NULL,
  student_number VARCHAR(30) NOT NULL,
  linked_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_temporary_claimant PRIMARY KEY (temporary_claimant_id),
  CONSTRAINT uk_temporary_claimant_student_number UNIQUE (student_number),
  CONSTRAINT fk_temp_claimant_department FOREIGN KEY (department_id) REFERENCES department (department_id) ON DELETE SET NULL,
  CONSTRAINT fk_temp_claimant_user FOREIGN KEY (linked_user_id) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT ck_temp_claimant_link CHECK ((linked_user_id IS NULL AND linked_at IS NULL) OR (linked_user_id IS NOT NULL AND linked_at IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE item_claim (
  item_claim_id BIGINT NOT NULL AUTO_INCREMENT,
  stored_item_id BIGINT NOT NULL,
  claimant_user_id BIGINT NULL,
  temporary_claimant_id BIGINT NULL,
  reviewed_by BIGINT NULL,
  request_method VARCHAR(30) NOT NULL,
  claim_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  ownership_description TEXT NULL,
  expected_lost_location VARCHAR(255) NULL,
  expected_lost_at DATETIME(6) NULL,
  rejection_reason TEXT NULL,
  closure_reason TEXT NULL,
  approved_at DATETIME(6) NULL,
  collected_at DATETIME(6) NULL,
  canceled_at DATETIME(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_item_claim PRIMARY KEY (item_claim_id),
  CONSTRAINT fk_item_claim_item FOREIGN KEY (stored_item_id) REFERENCES stored_item (stored_item_id) ON DELETE RESTRICT,
  CONSTRAINT fk_item_claim_user FOREIGN KEY (claimant_user_id) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT fk_item_claim_temp FOREIGN KEY (temporary_claimant_id) REFERENCES temporary_claimant (temporary_claimant_id) ON DELETE RESTRICT,
  CONSTRAINT fk_item_claim_reviewer FOREIGN KEY (reviewed_by) REFERENCES app_user (user_id) ON DELETE SET NULL,
  CONSTRAINT ck_item_claim_xor CHECK ((claimant_user_id IS NOT NULL AND temporary_claimant_id IS NULL) OR (claimant_user_id IS NULL AND temporary_claimant_id IS NOT NULL)),
  CONSTRAINT ck_item_claim_method CHECK (request_method IN ('ONLINE','ON_SITE_MEMBER','ON_SITE_TEMPORARY')),
  CONSTRAINT ck_item_claim_method_owner CHECK ((request_method IN ('ONLINE','ON_SITE_MEMBER') AND claimant_user_id IS NOT NULL) OR (request_method = 'ON_SITE_TEMPORARY' AND temporary_claimant_id IS NOT NULL)),
  CONSTRAINT ck_item_claim_status CHECK (claim_status IN ('PENDING','ADDITIONAL_INFO_REQUESTED','APPROVED','REJECTED','CANCELED','COLLECTED','CLOSED_BY_OTHER_COLLECTION','CLOSED_BY_STORAGE_END')),
  CONSTRAINT ck_item_claim_approved CHECK (claim_status <> 'APPROVED' OR approved_at IS NOT NULL),
  CONSTRAINT ck_item_claim_collected CHECK (claim_status <> 'COLLECTED' OR collected_at IS NOT NULL),
  CONSTRAINT ck_item_claim_canceled CHECK (claim_status <> 'CANCELED' OR canceled_at IS NOT NULL),
  CONSTRAINT ck_item_claim_version CHECK (version >= 0)
) ENGINE=InnoDB;
CREATE INDEX idx_item_claim_item_status ON item_claim (stored_item_id, claim_status);
CREATE INDEX idx_item_claim_user ON item_claim (claimant_user_id);
CREATE INDEX idx_item_claim_temp ON item_claim (temporary_claimant_id);

CREATE TABLE claim_message (
  claim_message_id BIGINT NOT NULL AUTO_INCREMENT,
  item_claim_id BIGINT NOT NULL,
  author_user_id BIGINT NULL,
  message_type VARCHAR(30) NOT NULL,
  content TEXT NOT NULL,
  is_internal BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_claim_message PRIMARY KEY (claim_message_id),
  CONSTRAINT fk_claim_message_claim FOREIGN KEY (item_claim_id) REFERENCES item_claim (item_claim_id) ON DELETE CASCADE,
  CONSTRAINT fk_claim_message_author FOREIGN KEY (author_user_id) REFERENCES app_user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE claim_verification (
  claim_verification_id BIGINT NOT NULL AUTO_INCREMENT,
  item_claim_id BIGINT NOT NULL,
  verified_by BIGINT NULL,
  verification_type VARCHAR(40) NOT NULL,
  verification_result VARCHAR(20) NOT NULL,
  verification_note TEXT NULL,
  verified_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_claim_verification PRIMARY KEY (claim_verification_id),
  CONSTRAINT fk_claim_verification_claim FOREIGN KEY (item_claim_id) REFERENCES item_claim (item_claim_id) ON DELETE CASCADE,
  CONSTRAINT fk_claim_verification_user FOREIGN KEY (verified_by) REFERENCES app_user (user_id) ON DELETE SET NULL,
  CONSTRAINT ck_claim_verification_type CHECK (verification_type IN ('STUDENT_ID','STUDENT_NUMBER','PRIVATE_FEATURE','LOST_LOCATION_TIME','DEVICE_UNLOCK','INTERNAL_CONTENTS','OTHER')),
  CONSTRAINT ck_claim_verification_result CHECK (verification_result IN ('PASSED','FAILED','PARTIAL'))
) ENGINE=InnoDB;

CREATE TABLE item_status_history (
  item_status_history_id BIGINT NOT NULL AUTO_INCREMENT,
  stored_item_id BIGINT NOT NULL,
  changed_by BIGINT NULL,
  actor_type VARCHAR(20) NOT NULL DEFAULT 'USER',
  previous_status VARCHAR(30) NULL,
  new_status VARCHAR(30) NOT NULL,
  change_reason TEXT NULL,
  changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_item_status_history PRIMARY KEY (item_status_history_id),
  CONSTRAINT fk_item_status_history_item FOREIGN KEY (stored_item_id) REFERENCES stored_item (stored_item_id) ON DELETE RESTRICT,
  CONSTRAINT fk_item_status_history_user FOREIGN KEY (changed_by) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT ck_item_status_history_actor CHECK ((actor_type='USER' AND changed_by IS NOT NULL) OR (actor_type='SYSTEM' AND changed_by IS NULL))
) ENGINE=InnoDB;

CREATE TABLE claim_status_history (
  claim_status_history_id BIGINT NOT NULL AUTO_INCREMENT,
  item_claim_id BIGINT NOT NULL,
  changed_by BIGINT NULL,
  actor_type VARCHAR(20) NOT NULL DEFAULT 'USER',
  previous_status VARCHAR(40) NULL,
  new_status VARCHAR(40) NOT NULL,
  change_reason TEXT NULL,
  changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_claim_status_history PRIMARY KEY (claim_status_history_id),
  CONSTRAINT fk_claim_status_history_claim FOREIGN KEY (item_claim_id) REFERENCES item_claim (item_claim_id) ON DELETE RESTRICT,
  CONSTRAINT fk_claim_status_history_user FOREIGN KEY (changed_by) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT ck_claim_status_history_actor CHECK ((actor_type='USER' AND changed_by IS NOT NULL) OR (actor_type='SYSTEM' AND changed_by IS NULL))
) ENGINE=InnoDB;

CREATE TABLE stored_item_attachment (
  attachment_id BIGINT NOT NULL AUTO_INCREMENT,
  stored_item_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT pk_stored_item_attachment PRIMARY KEY (attachment_id),
  CONSTRAINT uk_stored_item_attachment UNIQUE (stored_item_id, file_id),
  CONSTRAINT fk_stored_item_attachment_item FOREIGN KEY (stored_item_id) REFERENCES stored_item (stored_item_id) ON DELETE CASCADE,
  CONSTRAINT fk_stored_item_attachment_file FOREIGN KEY (file_id) REFERENCES file_resource (file_id) ON DELETE RESTRICT,
  CONSTRAINT ck_stored_item_attachment_order CHECK (display_order >= 0)
) ENGINE=InnoDB;

CREATE TABLE item_claim_attachment (
  attachment_id BIGINT NOT NULL AUTO_INCREMENT,
  item_claim_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  CONSTRAINT pk_item_claim_attachment PRIMARY KEY (attachment_id),
  CONSTRAINT uk_item_claim_attachment UNIQUE (item_claim_id, file_id),
  CONSTRAINT fk_item_claim_attachment_claim FOREIGN KEY (item_claim_id) REFERENCES item_claim (item_claim_id) ON DELETE CASCADE,
  CONSTRAINT fk_item_claim_attachment_file FOREIGN KEY (file_id) REFERENCES file_resource (file_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE claim_message_attachment (
  attachment_id BIGINT NOT NULL AUTO_INCREMENT,
  claim_message_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  CONSTRAINT pk_claim_message_attachment PRIMARY KEY (attachment_id),
  CONSTRAINT uk_claim_message_attachment UNIQUE (claim_message_id, file_id),
  CONSTRAINT fk_claim_message_attachment_message FOREIGN KEY (claim_message_id) REFERENCES claim_message (claim_message_id) ON DELETE CASCADE,
  CONSTRAINT fk_claim_message_attachment_file FOREIGN KEY (file_id) REFERENCES file_resource (file_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE request_category (
  request_category_id BIGINT NOT NULL AUTO_INCREMENT,
  category_name VARCHAR(100) NOT NULL,
  category_type VARCHAR(30) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT pk_request_category PRIMARY KEY (request_category_id),
  CONSTRAINT uk_request_category_name UNIQUE (category_name)
) ENGINE=InnoDB;

CREATE TABLE service_request (
  service_request_id BIGINT NOT NULL AUTO_INCREMENT,
  request_category_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  requester_user_id BIGINT NOT NULL,
  receipt_number VARCHAR(50) NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT NOT NULL,
  equipment_name VARCHAR(150) NULL,
  visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
  request_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  completed_at DATETIME(6) NULL,
  CONSTRAINT pk_service_request PRIMARY KEY (service_request_id),
  CONSTRAINT uk_service_request_receipt UNIQUE (receipt_number),
  CONSTRAINT fk_service_request_category FOREIGN KEY (request_category_id) REFERENCES request_category (request_category_id) ON DELETE RESTRICT,
  CONSTRAINT fk_service_request_location FOREIGN KEY (location_id) REFERENCES location (location_id) ON DELETE RESTRICT,
  CONSTRAINT fk_service_request_requester FOREIGN KEY (requester_user_id) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT ck_service_request_visibility CHECK (visibility IN ('PUBLIC','PRIVATE')),
  CONSTRAINT ck_service_request_status CHECK (request_status IN ('RECEIVED','ASSIGNED','CHECKING','ADDITIONAL_INFO_REQUESTED','SCHEDULED','IN_PROGRESS','COMPLETED','UNAVAILABLE','REJECTED','CANCELED')),
  CONSTRAINT ck_service_request_completed CHECK (request_status <> 'COMPLETED' OR completed_at IS NOT NULL),
  CONSTRAINT ck_service_request_version CHECK (version >= 0)
) ENGINE=InnoDB;
CREATE INDEX idx_service_request_status ON service_request (request_status, created_at);
CREATE INDEX idx_service_request_requester ON service_request (requester_user_id, created_at);

CREATE TABLE request_assignment (
  request_assignment_id BIGINT NOT NULL AUTO_INCREMENT,
  service_request_id BIGINT NOT NULL,
  assigned_department_id BIGINT NOT NULL,
  assigned_user_id BIGINT NULL,
  assigned_by BIGINT NULL,
  assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ended_at DATETIME(6) NULL,
  current_marker TINYINT GENERATED ALWAYS AS (CASE WHEN ended_at IS NULL THEN 1 ELSE NULL END) STORED,
  CONSTRAINT pk_request_assignment PRIMARY KEY (request_assignment_id),
  CONSTRAINT uk_request_assignment_current UNIQUE (service_request_id, current_marker),
  CONSTRAINT fk_request_assignment_request FOREIGN KEY (service_request_id) REFERENCES service_request (service_request_id) ON DELETE RESTRICT,
  CONSTRAINT fk_request_assignment_department FOREIGN KEY (assigned_department_id) REFERENCES department (department_id) ON DELETE RESTRICT,
  CONSTRAINT fk_request_assignment_user FOREIGN KEY (assigned_user_id) REFERENCES app_user (user_id) ON DELETE SET NULL,
  CONSTRAINT fk_request_assignment_assigned_by FOREIGN KEY (assigned_by) REFERENCES app_user (user_id) ON DELETE SET NULL,
  CONSTRAINT ck_request_assignment_period CHECK (ended_at IS NULL OR ended_at >= assigned_at)
) ENGINE=InnoDB;

CREATE TABLE request_comment (
  request_comment_id BIGINT NOT NULL AUTO_INCREMENT,
  service_request_id BIGINT NOT NULL,
  author_user_id BIGINT NULL,
  comment_type VARCHAR(30) NOT NULL,
  content TEXT NOT NULL,
  is_internal BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_request_comment PRIMARY KEY (request_comment_id),
  CONSTRAINT fk_request_comment_request FOREIGN KEY (service_request_id) REFERENCES service_request (service_request_id) ON DELETE CASCADE,
  CONSTRAINT fk_request_comment_author FOREIGN KEY (author_user_id) REFERENCES app_user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE request_status_history (
  request_status_history_id BIGINT NOT NULL AUTO_INCREMENT,
  service_request_id BIGINT NOT NULL,
  changed_by BIGINT NULL,
  actor_type VARCHAR(20) NOT NULL DEFAULT 'USER',
  previous_status VARCHAR(30) NULL,
  new_status VARCHAR(30) NOT NULL,
  change_reason TEXT NULL,
  changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_request_status_history PRIMARY KEY (request_status_history_id),
  CONSTRAINT fk_request_status_history_request FOREIGN KEY (service_request_id) REFERENCES service_request (service_request_id) ON DELETE RESTRICT,
  CONSTRAINT fk_request_status_history_user FOREIGN KEY (changed_by) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT ck_request_status_history_actor CHECK ((actor_type='USER' AND changed_by IS NOT NULL) OR (actor_type='SYSTEM' AND changed_by IS NULL))
) ENGINE=InnoDB;

CREATE TABLE service_request_attachment (
  attachment_id BIGINT NOT NULL AUTO_INCREMENT,
  service_request_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  CONSTRAINT pk_service_request_attachment PRIMARY KEY (attachment_id),
  CONSTRAINT uk_service_request_attachment UNIQUE (service_request_id, file_id),
  CONSTRAINT fk_service_request_attachment_request FOREIGN KEY (service_request_id) REFERENCES service_request (service_request_id) ON DELETE CASCADE,
  CONSTRAINT fk_service_request_attachment_file FOREIGN KEY (file_id) REFERENCES file_resource (file_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE request_comment_attachment (
  attachment_id BIGINT NOT NULL AUTO_INCREMENT,
  request_comment_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  CONSTRAINT pk_request_comment_attachment PRIMARY KEY (attachment_id),
  CONSTRAINT uk_request_comment_attachment UNIQUE (request_comment_id, file_id),
  CONSTRAINT fk_request_comment_attachment_comment FOREIGN KEY (request_comment_id) REFERENCES request_comment (request_comment_id) ON DELETE CASCADE,
  CONSTRAINT fk_request_comment_attachment_file FOREIGN KEY (file_id) REFERENCES file_resource (file_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE notification (
  notification_id BIGINT NOT NULL AUTO_INCREMENT,
  recipient_user_id BIGINT NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  reference_type VARCHAR(50) NULL,
  reference_id BIGINT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  delivery_channel VARCHAR(30) NOT NULL DEFAULT 'WEB',
  delivery_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  event_key VARCHAR(150) NULL,
  read_at DATETIME(6) NULL,
  sent_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_notification PRIMARY KEY (notification_id),
  CONSTRAINT uk_notification_event_key UNIQUE (event_key),
  CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT ck_notification_reference CHECK ((reference_type IS NULL AND reference_id IS NULL) OR (reference_type IS NOT NULL AND reference_id IS NOT NULL))
) ENGINE=InnoDB;
CREATE INDEX idx_notification_recipient ON notification (recipient_user_id, read_at, created_at);
CREATE INDEX idx_notification_reference ON notification (reference_type, reference_id);

CREATE TABLE audit_log (
  audit_log_id BIGINT NOT NULL AUTO_INCREMENT,
  actor_user_id BIGINT NULL,
  actor_type VARCHAR(20) NOT NULL DEFAULT 'USER',
  target_type VARCHAR(50) NOT NULL,
  target_id BIGINT NULL,
  target_display_value VARCHAR(255) NULL,
  action_type VARCHAR(50) NOT NULL,
  before_value JSON NULL,
  after_value JSON NULL,
  action_reason TEXT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_audit_log PRIMARY KEY (audit_log_id),
  CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (user_id) ON DELETE RESTRICT,
  CONSTRAINT ck_audit_log_actor CHECK ((actor_type='USER' AND actor_user_id IS NOT NULL) OR (actor_type='SYSTEM' AND actor_user_id IS NULL))
) ENGINE=InnoDB;
CREATE INDEX idx_audit_log_target ON audit_log (target_type, target_id, created_at);

INSERT INTO app_role (role_code, role_name) VALUES
  ('STUDENT', '학생'),
  ('LOST_ITEM_STAFF', '분실물 담당자'),
  ('FACILITY_STAFF', '시설·기자재 담당자'),
  ('ADMIN', '관리자');

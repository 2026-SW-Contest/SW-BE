-- Postman search fixture for the "postman" Spring profile only.
-- The rows created by this script use dedicated identifiers so the script
-- can be run repeatedly without accumulating duplicate fixture data.

INSERT INTO department (
    department_name,
    department_type,
    is_active
)
VALUES (
    'Postman 검색 테스트 부서',
    'ADMIN',
    TRUE
)
ON DUPLICATE KEY UPDATE
    department_type = 'ADMIN',
    is_active = TRUE;

SET @postman_department_id = (
    SELECT department_id
    FROM department
    WHERE department_name = 'Postman 검색 테스트 부서'
);

INSERT INTO app_user (
    department_id,
    email,
    password_hash,
    name,
    student_number,
    account_status,
    email_verified
)
VALUES (
    @postman_department_id,
    'postman-search@mju.ac.kr',
    '{noop}postman1234',
    'Postman 검색 사용자',
    'POSTMAN-SEARCH-001',
    'ACTIVE',
    TRUE
)
ON DUPLICATE KEY UPDATE
    department_id = @postman_department_id,
    password_hash = '{noop}postman1234',
    name = 'Postman 검색 사용자',
    account_status = 'ACTIVE',
    email_verified = TRUE;

SET @postman_user_id = (
    SELECT user_id
    FROM app_user
    WHERE email = 'postman-search@mju.ac.kr'
);

SET @student_role_id = (
    SELECT role_id
    FROM app_role
    WHERE role_code = 'STUDENT'
);

INSERT INTO user_role (
    user_id,
    role_id,
    granted_by
)
SELECT
    @postman_user_id,
    @student_role_id,
    @postman_user_id
WHERE NOT EXISTS (
    SELECT 1
    FROM user_role
    WHERE user_id = @postman_user_id
      AND role_id = @student_role_id
      AND revoked_at IS NULL
);

SET @postman_location_id = (
    SELECT MIN(location_id)
    FROM location
);

SET @postman_building_id = (
    SELECT building_id
    FROM location
    WHERE location_id = @postman_location_id
);

INSERT INTO lost_item_office (
    building_id,
    department_id,
    location_id,
    office_name,
    operating_hours,
    guidance,
    is_primary,
    is_active
)
VALUES (
    @postman_building_id,
    @postman_department_id,
    @postman_location_id,
    'Postman 검색 테스트 보관소',
    '09:00~18:00',
    '검색 API 검증용 보관소입니다.',
    FALSE,
    TRUE
)
ON DUPLICATE KEY UPDATE
    department_id = @postman_department_id,
    location_id = @postman_location_id,
    is_active = TRUE;

SET @postman_office_id = (
    SELECT office_id
    FROM lost_item_office
    WHERE building_id = @postman_building_id
      AND office_name = 'Postman 검색 테스트 보관소'
);

INSERT INTO item_category (
    category_name,
    is_important_item,
    default_storage_days,
    is_active
)
VALUES (
    'Postman 전자기기',
    FALSE,
    90,
    TRUE
)
ON DUPLICATE KEY UPDATE
    default_storage_days = 90,
    is_active = TRUE;

SET @postman_item_category_id = (
    SELECT item_category_id
    FROM item_category
    WHERE category_name = 'Postman 전자기기'
);

DELETE FROM facility_request
WHERE receipt_number LIKE 'POSTMAN-SEARCH-%';

DELETE FROM stored_item
WHERE private_description LIKE 'POSTMAN_SEARCH_FIXTURE:%';

DELETE FROM recent_search
WHERE user_id = @postman_user_id;

INSERT INTO stored_item (
    office_id,
    found_location_id,
    registered_by,
    item_category_id,
    item_name,
    public_status,
    public_description,
    private_description,
    found_date,
    found_time,
    found_time_unknown,
    received_at,
    storage_position,
    storage_deadline,
    created_at,
    updated_at
)
VALUES
    (
        @postman_office_id,
        @postman_location_id,
        @postman_user_id,
        @postman_item_category_id,
        '에어팟 프로',
        'STORED',
        '흰색 충전 케이스와 함께 발견된 에어팟 프로입니다.',
        'POSTMAN_SEARCH_FIXTURE:LOST_ITEM_1',
        CURRENT_DATE - INTERVAL 1 DAY,
        NULL,
        TRUE,
        CURRENT_TIMESTAMP - INTERVAL 1 DAY,
        '테스트 선반 A-1',
        CURRENT_DATE + INTERVAL 89 DAY,
        CURRENT_TIMESTAMP - INTERVAL 1 HOUR,
        CURRENT_TIMESTAMP - INTERVAL 1 HOUR
    ),
    (
        @postman_office_id,
        @postman_location_id,
        @postman_user_id,
        @postman_item_category_id,
        '검은색 에어팟 케이스',
        'CHECKING',
        '검은색 보호 케이스가 씌워진 에어팟 충전 케이스입니다.',
        'POSTMAN_SEARCH_FIXTURE:LOST_ITEM_2',
        CURRENT_DATE - INTERVAL 2 DAY,
        NULL,
        TRUE,
        CURRENT_TIMESTAMP - INTERVAL 2 DAY,
        '테스트 선반 A-2',
        CURRENT_DATE + INTERVAL 88 DAY,
        CURRENT_TIMESTAMP - INTERVAL 2 HOUR,
        CURRENT_TIMESTAMP - INTERVAL 2 HOUR
    ),
    (
        @postman_office_id,
        @postman_location_id,
        @postman_user_id,
        @postman_item_category_id,
        '갈색 카드지갑',
        'STORED',
        '학생증이 없는 갈색 카드지갑입니다.',
        'POSTMAN_SEARCH_FIXTURE:LOST_ITEM_3',
        CURRENT_DATE - INTERVAL 3 DAY,
        NULL,
        TRUE,
        CURRENT_TIMESTAMP - INTERVAL 3 DAY,
        '테스트 선반 B-1',
        CURRENT_DATE + INTERVAL 87 DAY,
        CURRENT_TIMESTAMP - INTERVAL 3 HOUR,
        CURRENT_TIMESTAMP - INTERVAL 3 HOUR
    );

SET @postman_facility_category_id = (
    SELECT facility_category_id
    FROM facility_category
    WHERE category_name = '냉난방/온도'
);

INSERT INTO facility_request (
    facility_category_id,
    location_id,
    requester_user_id,
    receipt_number,
    title,
    description,
    equipment_name,
    visibility,
    request_status,
    created_at,
    updated_at
)
VALUES
    (
        @postman_facility_category_id,
        @postman_location_id,
        @postman_user_id,
        'POSTMAN-SEARCH-001',
        '강의실 에어컨 고장',
        '에어컨에서 찬 바람이 나오지 않습니다.',
        '천장형 에어컨',
        'PUBLIC',
        'RECEIVED',
        CURRENT_TIMESTAMP - INTERVAL 30 MINUTE,
        CURRENT_TIMESTAMP - INTERVAL 30 MINUTE
    ),
    (
        @postman_facility_category_id,
        @postman_location_id,
        @postman_user_id,
        'POSTMAN-SEARCH-002',
        '에어컨 소음 점검 요청',
        '가동 중 큰 소음이 발생합니다.',
        '스탠드형 에어컨',
        'PUBLIC',
        'CHECKING',
        CURRENT_TIMESTAMP - INTERVAL 1 HOUR,
        CURRENT_TIMESTAMP - INTERVAL 1 HOUR
    ),
    (
        @postman_facility_category_id,
        @postman_location_id,
        @postman_user_id,
        'POSTMAN-SEARCH-003',
        '비공개 에어컨 문의',
        '검색 결과와 추천 검색어에서 제외되어야 합니다.',
        '벽걸이 에어컨',
        'PRIVATE',
        'RECEIVED',
        CURRENT_TIMESTAMP - INTERVAL 2 HOUR,
        CURRENT_TIMESTAMP - INTERVAL 2 HOUR
    ),
    (
        @postman_facility_category_id,
        @postman_location_id,
        @postman_user_id,
        'POSTMAN-SEARCH-004',
        '강의실 난방 점검 요청',
        '난방 온도가 충분히 올라가지 않습니다.',
        '라디에이터',
        'PUBLIC',
        'RECEIVED',
        CURRENT_TIMESTAMP - INTERVAL 3 HOUR,
        CURRENT_TIMESTAMP - INTERVAL 3 HOUR
    );

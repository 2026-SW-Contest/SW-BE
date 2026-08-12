-- Seed selectable lost-item offices for the humanities campus.
-- Office candidates are based on official campus department and security
-- locations. Academic offices are kept non-primary because they are
-- supplemental drop-off candidates rather than building-wide offices.

INSERT INTO department (department_name, department_type)
SELECT source.department_name, source.department_type
FROM (
    SELECT '인문학생지원팀' AS department_name,
           'ADMINISTRATION' AS department_type
    UNION ALL
    SELECT '방목기초교육대학 교학팀', 'ACADEMIC_AFFAIRS'
    UNION ALL
    SELECT '인공지능·소프트웨어융합대학 교학팀', 'ACADEMIC_AFFAIRS'
    UNION ALL
    SELECT '사회과학대학 교학팀', 'ACADEMIC_AFFAIRS'
    UNION ALL
    SELECT '미디어·휴먼라이프대학 교학팀', 'ACADEMIC_AFFAIRS'
    UNION ALL
    SELECT '인문대학 교학팀', 'ACADEMIC_AFFAIRS'
    UNION ALL
    SELECT '미래융합대학 교학팀', 'ACADEMIC_AFFAIRS'
    UNION ALL
    SELECT '아너칼리지 교학팀', 'ACADEMIC_AFFAIRS'
    UNION ALL
    SELECT '경영대학 교학팀', 'ACADEMIC_AFFAIRS'
) source
WHERE NOT EXISTS (
    SELECT 1
    FROM department d
    WHERE d.department_name = source.department_name
);

INSERT INTO location (
    building_id,
    parent_location_id,
    location_name,
    floor,
    description
)
SELECT b.building_id,
       root.location_id,
       source.location_name,
       source.floor,
       source.description
FROM (
    SELECT 'S1' AS building_code,
           '인문학생지원팀' AS location_name,
           '2층' AS floor,
           '인문캠퍼스 학생 지원 및 분실물 문의' AS description
    UNION ALL
    SELECT 'S1', '종합관 경비 상황실', NULL,
           '건물 경비 근무지 및 분실물 보관 후보'
    UNION ALL
    SELECT 'S2', '학생회관 경비실', NULL,
           '건물 경비 근무지 및 분실물 보관 후보'
    UNION ALL
    SELECT 'S3', '미래관 경비실', NULL,
           '건물 경비 근무지 및 분실물 보관 후보'
    UNION ALL
    SELECT 'S4', '국제관 경비실', NULL,
           '건물 경비 근무지 및 분실물 보관 후보'
    UNION ALL
    SELECT 'S5', '행정동 경비실', '1층',
           '건물 경비 근무지 및 분실물 보관 후보'
    UNION ALL
    SELECT 'S9', '도서관 경비실', NULL,
           '건물 경비 근무지 및 분실물 보관 후보'
    UNION ALL
    SELECT 'S10', 'MCC 경비실', '1층',
           '건물 경비 근무지 및 분실물 보관 후보'
    UNION ALL
    SELECT 'S1', '방목기초교육대학 교학팀', '1층',
           '단과대학 학사 행정 및 학생 지원'
    UNION ALL
    SELECT 'S1', '인공지능·소프트웨어융합대학 교학팀', '3층',
           '단과대학 학사 행정 및 학생 지원'
    UNION ALL
    SELECT 'S1', '사회과학대학 교학팀', '6층',
           '단과대학 학사 행정 및 학생 지원'
    UNION ALL
    SELECT 'S1', '미디어·휴먼라이프대학 교학팀', '6층',
           '단과대학 학사 행정 및 학생 지원'
    UNION ALL
    SELECT 'S1', '인문대학 교학팀', '7층',
           '단과대학 학사 행정 및 학생 지원'
    UNION ALL
    SELECT 'S3', '미래융합대학 교학팀', '2층',
           '단과대학 학사 행정 및 학생 지원'
    UNION ALL
    SELECT 'S10', '아너칼리지 교학팀', '3층',
           '단과대학 학사 행정 및 학생 지원'
    UNION ALL
    SELECT 'S10', '경영대학 교학팀', '4층',
           '단과대학 학사 행정 및 학생 지원'
) source
JOIN building b
  ON b.building_code = source.building_code
JOIN location root
  ON root.building_id = b.building_id
 AND root.location_name = b.building_name
WHERE NOT EXISTS (
    SELECT 1
    FROM location l
    WHERE l.building_id = b.building_id
      AND l.location_name = source.location_name
);

INSERT INTO lost_item_office (
    building_id,
    department_id,
    location_id,
    office_name,
    operating_hours,
    guidance,
    is_primary
)
SELECT b.building_id,
       d.department_id,
       l.location_id,
       '인문학생지원팀 분실물 보관소',
       '평일 09:00~17:30',
       '분실물 관련 문의: 02-300-1521',
       CASE WHEN EXISTS (
           SELECT 1
           FROM lost_item_office existing
           WHERE existing.building_id = b.building_id
             AND existing.is_primary = TRUE
             AND existing.is_active = TRUE
       ) THEN FALSE ELSE TRUE END
FROM building b
JOIN location l
  ON l.building_id = b.building_id
 AND l.location_name = '인문학생지원팀'
JOIN department d
  ON d.department_name = '인문학생지원팀'
WHERE b.building_code = 'S1'
  AND NOT EXISTS (
      SELECT 1
      FROM lost_item_office existing
      WHERE existing.building_id = b.building_id
        AND existing.office_name = '인문학생지원팀 분실물 보관소'
  );

INSERT INTO lost_item_office (
    building_id,
    department_id,
    location_id,
    office_name,
    operating_hours,
    guidance,
    is_primary
)
SELECT b.building_id,
       d.department_id,
       l.location_id,
       source.office_name,
       '24시간 교대근무',
       '건물에서 습득한 물품의 임시 보관 후보 장소',
       CASE WHEN source.primary_candidate = TRUE
           AND NOT EXISTS (
               SELECT 1
               FROM lost_item_office existing
               WHERE existing.building_id = b.building_id
                 AND existing.is_primary = TRUE
                 AND existing.is_active = TRUE
           )
           THEN TRUE ELSE FALSE END
FROM (
    SELECT 'S1' AS building_code,
           '종합관 경비 상황실' AS office_name,
           FALSE AS primary_candidate
    UNION ALL
    SELECT 'S2', '학생회관 경비실', TRUE
    UNION ALL
    SELECT 'S3', '미래관 경비실', TRUE
    UNION ALL
    SELECT 'S4', '국제관 경비실', TRUE
    UNION ALL
    SELECT 'S5', '행정동 경비실', TRUE
    UNION ALL
    SELECT 'S9', '도서관 경비실', TRUE
    UNION ALL
    SELECT 'S10', 'MCC 경비실', TRUE
) source
JOIN building b
  ON b.building_code = source.building_code
JOIN location l
  ON l.building_id = b.building_id
 AND l.location_name = source.office_name
JOIN department d
  ON d.department_name = '인문학생지원팀'
WHERE NOT EXISTS (
    SELECT 1
    FROM lost_item_office existing
    WHERE existing.building_id = b.building_id
      AND existing.office_name = source.office_name
);

INSERT INTO lost_item_office (
    building_id,
    department_id,
    location_id,
    office_name,
    operating_hours,
    guidance,
    is_primary
)
SELECT b.building_id,
       d.department_id,
       l.location_id,
       source.office_name,
       '평일 운영(학사 일정에 따라 변동)',
       '소속 단과대학 관련 습득물의 임시 보관 후보 장소입니다. 방문 전 교학팀에 보관 여부를 확인해 주세요.',
       FALSE
FROM (
    SELECT 'S1' AS building_code,
           '방목기초교육대학 교학팀' AS office_name
    UNION ALL
    SELECT 'S1', '인공지능·소프트웨어융합대학 교학팀'
    UNION ALL
    SELECT 'S1', '사회과학대학 교학팀'
    UNION ALL
    SELECT 'S1', '미디어·휴먼라이프대학 교학팀'
    UNION ALL
    SELECT 'S1', '인문대학 교학팀'
    UNION ALL
    SELECT 'S3', '미래융합대학 교학팀'
    UNION ALL
    SELECT 'S10', '아너칼리지 교학팀'
    UNION ALL
    SELECT 'S10', '경영대학 교학팀'
) source
JOIN building b
  ON b.building_code = source.building_code
JOIN location l
  ON l.building_id = b.building_id
 AND l.location_name = source.office_name
JOIN department d
  ON d.department_name = source.office_name
WHERE NOT EXISTS (
    SELECT 1
    FROM lost_item_office existing
    WHERE existing.building_id = b.building_id
      AND existing.office_name = source.office_name
);

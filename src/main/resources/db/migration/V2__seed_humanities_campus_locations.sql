INSERT INTO campus (campus_name, address)
VALUES ('인문캠퍼스', NULL);

SET @humanities_campus_id = LAST_INSERT_ID();

INSERT INTO building (campus_id, building_name, building_code)
VALUES (@humanities_campus_id, '본관(종합관)', 'S1'),
       (@humanities_campus_id, '학생회관', 'S2'),
       (@humanities_campus_id, '미래관', 'S3'),
       (@humanities_campus_id, '경상관(국제관)', 'S4'),
       (@humanities_campus_id, '행정동', 'S5'),
       (@humanities_campus_id, '운동장', 'S6'),
       (@humanities_campus_id, '주차장', 'S7'),
       (@humanities_campus_id, '기숙사', 'S8'),
       (@humanities_campus_id, '방목학술정보관(도서관)', 'S9'),
       (@humanities_campus_id, 'MCC관', 'S10'),
       (@humanities_campus_id, '기타', NULL);

INSERT INTO location (location_id, building_id, location_name)
SELECT CASE building_code
           WHEN 'S1' THEN 1
           WHEN 'S2' THEN 2
           WHEN 'S3' THEN 3
           WHEN 'S4' THEN 4
           WHEN 'S5' THEN 5
           WHEN 'S6' THEN 6
           WHEN 'S7' THEN 7
           WHEN 'S8' THEN 8
           WHEN 'S9' THEN 9
           WHEN 'S10' THEN 10
           ELSE 11
       END,
       building_id,
       building_name
FROM building
WHERE campus_id = @humanities_campus_id
ORDER BY CASE building_code
             WHEN 'S1' THEN 1
             WHEN 'S2' THEN 2
             WHEN 'S3' THEN 3
             WHEN 'S4' THEN 4
             WHEN 'S5' THEN 5
             WHEN 'S6' THEN 6
             WHEN 'S7' THEN 7
             WHEN 'S8' THEN 8
             WHEN 'S9' THEN 9
             WHEN 'S10' THEN 10
             ELSE 11
         END;

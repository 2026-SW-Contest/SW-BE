ALTER TABLE item_category
    DROP COLUMN is_active;

INSERT INTO item_category (
    item_category_id,
    category_name
)
VALUES (1, '전자기기'),
       (2, '지갑/카드/현금'),
       (3, '의류/패션잡화'),
       (4, '가방/파우치'),
       (5, '도서/문구'),
       (6, '액세서리'),
       (7, '기타');

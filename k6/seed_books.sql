-- TRUNCATE 이후 단독 실행 가능 (member + character + book + book_page 전부 생성)
-- 실행: mysql -u <user> -p <dbname> < seed_books.sql

-- member 생성
INSERT IGNORE INTO member (username, password, role, auth_provider)
VALUES ('seed_user', 'dummy', 'USER', 'local');

-- character 10개 생성
INSERT IGNORE INTO main_character (name, personality, image_url, origin_image_url, user_description, appearance_keywords, member_id)
SELECT
    CONCAT('캐릭터', n),
    '밝고 용감한',
    CONCAT('https://example.com/char', n, '.png'),
    NULL,
    '부하테스트용',
    '파란 눈, 갈색 머리',
    (SELECT id FROM member WHERE username = 'seed_user')
FROM (
    SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
    UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
) nums
WHERE NOT EXISTS (
    SELECT 1 FROM main_character
    WHERE member_id = (SELECT id FROM member WHERE username = 'seed_user')
);

DROP PROCEDURE IF EXISTS seed_books;

DELIMITER $$

CREATE PROCEDURE seed_books()
BEGIN
    DECLARE i        INT     DEFAULT 1;
    DECLARE book_id  VARCHAR(36);
    DECLARE mem_id   BIGINT;
    DECLARE char_cnt BIGINT;
    DECLARE first_char_id BIGINT;

    SET mem_id = (SELECT id FROM member WHERE username = 'seed_user' LIMIT 1);

    IF mem_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'seed_user not found. Run seed.sql first.';
    END IF;

    SET char_cnt      = (SELECT COUNT(*) FROM main_character WHERE member_id = mem_id);
    SET first_char_id = (SELECT MIN(id)  FROM main_character WHERE member_id = mem_id);

    IF char_cnt = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No characters found for seed_user. Run seed.sql first.';
    END IF;

    WHILE i <= 50000 DO
        SET book_id = UUID();

        -- character_id 순환: first_char_id + (0 ~ char_cnt-1)
        INSERT INTO book (id, user_id, character_id, title, created_at, book_color, author, story_length, cover_image_url)
        VALUES (
            book_id,
            mem_id,
            first_char_id + ((i - 1) % char_cnt),
            CONCAT('테스트 책 ', i),
            NOW() - INTERVAL FLOOR(RAND() * 365) DAY,
            1,
            '테스트 작가',
            3,
            'https://example.com/cover.png'
        );

        INSERT INTO book_page (book_id, context, image_url, page_number) VALUES
            (book_id, CONCAT('1페이지 내용입니다. 책번호=', i), 'https://example.com/page1.png', 1),
            (book_id, CONCAT('2페이지 내용입니다. 책번호=', i), 'https://example.com/page2.png', 2),
            (book_id, CONCAT('3페이지 내용입니다. 책번호=', i), 'https://example.com/page3.png', 3);

        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL seed_books();
DROP PROCEDURE IF EXISTS seed_books;

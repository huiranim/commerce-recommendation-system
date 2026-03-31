-- api-server/src/main/resources/data.sql
-- 카테고리 시드
INSERT IGNORE INTO categories (id, name, parent_id) VALUES
    ('electronics', '전자제품', NULL),
    ('sports', '스포츠', NULL),
    ('fashion', '패션', NULL),
    ('laptop', '노트북', 'electronics'),
    ('phone', '스마트폰', 'electronics'),
    ('running', '러닝용품', 'sports');

-- 상품 시드
INSERT IGNORE INTO products (id, name, category_id, price, status) VALUES
    ('prod-001', 'LG그램 17인치', 'laptop', 1890000, 'ACTIVE'),
    ('prod-002', '삼성 갤럭시 S24', 'phone', 1200000, 'ACTIVE'),
    ('prod-003', '나이키 페가수스 41', 'running', 169000, 'ACTIVE'),
    ('prod-004', '아디다스 울트라부스트', 'running', 189000, 'ACTIVE'),
    ('prod-005', 'MacBook Pro M3', 'laptop', 2890000, 'ACTIVE'),
    ('prod-006', '아이폰 15 Pro', 'phone', 1550000, 'ACTIVE');

-- 유저 시드
INSERT IGNORE INTO users (id, name, email) VALUES
    ('user-001', '김철수', 'kim@example.com'),
    ('user-002', '이영희', 'lee@example.com'),
    ('user-003', '박민준', 'park@example.com');

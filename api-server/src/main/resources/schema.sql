CREATE TABLE IF NOT EXISTS categories (
    id         VARCHAR(50)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    parent_id  VARCHAR(50)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS users (
    id         VARCHAR(36)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
);

CREATE TABLE IF NOT EXISTS products (
    id          VARCHAR(36)   NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    category_id VARCHAR(50)   NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    status      VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_category_status (category_id, status)
);

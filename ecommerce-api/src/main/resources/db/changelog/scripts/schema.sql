CREATE
EXTENSION IF NOT EXISTS "pgcrypto";
CREATE
EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE product_categories
(
    id         SMALLINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_product_categories_id PRIMARY KEY (id),
    CONSTRAINT uq_product_categories_name UNIQUE (name)
);

CREATE UNIQUE INDEX uq_product_categories_name_lower ON product_categories (LOWER(name));

INSERT INTO product_categories (name)
VALUES ('Accessories'),
       ('Beauty'),
       ('Books'),
       ('Clothing'),
       ('Electronics'),
       ('Food & Beverage'),
       ('Footwear'),
       ('Games'),
       ('Gifts'),
       ('Health'),
       ('Home & Office'),
       ('Kitchen'),
       ('Misc'),
       ('Outdoors'),
       ('Pets'),
       ('Sports'),
       ('Stationery'),
       ('Tools');

CREATE TABLE products
(
    id            UUID                    DEFAULT gen_random_uuid() NOT NULL,
    sku           VARCHAR(10)    NOT NULL,
    name          VARCHAR(255)   NOT NULL,
    description   TEXT,
    category_id   SMALLINT       NOT NULL,
    price         NUMERIC(12, 2) NOT NULL,
    stock         INTEGER        NOT NULL DEFAULT 0,
    weight_kg     NUMERIC(8, 3),
    version       BIGINT         NOT NULL DEFAULT 0,
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_products_id PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT chk_products_sku_format CHECK (sku ~ '^[A-Z0-9]{1,10}$'),
    CONSTRAINT chk_products_price_positive CHECK (price > 0),
    CONSTRAINT chk_products_stock_non_negative CHECK (stock >= 0),
    CONSTRAINT chk_products_weight_positive CHECK (weight_kg IS NULL OR weight_kg >= 0),
    CONSTRAINT chk_products_soft_delete_consistency CHECK (is_active = (deleted_at IS NULL)),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES product_categories (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_products_name_trgm_gin ON products USING GIN (LOWER(name) gin_trgm_ops) WHERE is_active;
CREATE INDEX idx_products_sku_trgm_gin ON products USING GIN (sku gin_trgm_ops) WHERE is_active;

CREATE INDEX idx_products_active_created_id ON products (created_at DESC, id DESC) WHERE is_active;
CREATE INDEX idx_products_active_price_id ON products (price, id) WHERE is_active;
CREATE INDEX idx_products_active_name_id ON products (name, id) WHERE is_active;
CREATE INDEX idx_products_active_category_price ON products (category_id, price) WHERE is_active;
CREATE INDEX idx_products_active_stock ON products (stock) WHERE is_active;
CREATE INDEX idx_products_active_weight ON products (weight_kg) WHERE is_active AND weight_kg IS NOT NULL;

CREATE TABLE purchase_orders
(
    id           UUID                    DEFAULT gen_random_uuid() NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    version      BIGINT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_purchase_orders_id PRIMARY KEY (id),
    CONSTRAINT chk_purchase_orders_total_non_negative CHECK (total_amount >= 0)
);

CREATE INDEX idx_purchase_orders_created_at ON purchase_orders (created_at DESC);

CREATE TABLE purchase_order_items
(
    id              UUID                    DEFAULT gen_random_uuid() NOT NULL,
    order_id        UUID           NOT NULL,
    product_id      UUID           NOT NULL,
    quantity        INTEGER        NOT NULL,
    subtotal_amount NUMERIC(12, 2) NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_purchase_order_items_id PRIMARY KEY (id),
    CONSTRAINT fk_purchase_order_items_order FOREIGN KEY (order_id)
        REFERENCES purchase_orders (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_purchase_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_order_items_order_product UNIQUE (order_id, product_id),
    CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_items_subtotal_non_negative CHECK (subtotal_amount >= 0)
);

CREATE INDEX idx_purchase_order_items_product_id ON purchase_order_items (product_id);

CREATE
OR REPLACE FUNCTION update_timestamp_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE
    ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp_column();

CREATE TRIGGER trg_purchase_orders_updated_at
    BEFORE UPDATE
    ON purchase_orders
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp_column();

CREATE TRIGGER trg_purchase_order_items_updated_at
    BEFORE UPDATE
    ON purchase_order_items
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp_column();

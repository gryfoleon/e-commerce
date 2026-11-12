INSERT INTO products (id, sku, name, description, category_id, price, stock, weight_kg, is_active, deleted_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'LAPTOP01', 'Laptop Pro 15', 'High-end laptop',
        (SELECT id FROM product_categories WHERE name = 'Electronics'), 1499.99, 10, 1.800, TRUE, NULL),
       ('22222222-2222-2222-2222-222222222222', 'MOUSE001', 'Wireless Mouse', 'Ergonomic wireless mouse',
        (SELECT id FROM product_categories WHERE name = 'Electronics'), 25.50, 100, 0.090, TRUE, NULL),
       ('33333333-3333-3333-3333-333333333333', 'BOOK0001', 'Clean Code', NULL,
        (SELECT id FROM product_categories WHERE name = 'Books'), 39.90, 50, NULL, TRUE, NULL),
       ('44444444-4444-4444-4444-444444444444', 'OLDITEM1', 'Discontinued Keyboard', 'Soft-deleted product',
        (SELECT id FROM product_categories WHERE name = 'Electronics'), 15.00, 0, 0.700, FALSE, CURRENT_TIMESTAMP);

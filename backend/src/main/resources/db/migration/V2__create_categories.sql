CREATE TABLE categories (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    color       VARCHAR(7),
    is_default  BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO categories (name, color, is_default) VALUES
    ('Supermercado',    '#4CAF50', TRUE),
    ('Restaurantes',    '#FF9800', TRUE),
    ('Transportes',     '#2196F3', TRUE),
    ('Saúde',           '#F44336', TRUE),
    ('Lazer',           '#9C27B0', TRUE),
    ('Habitação',       '#795548', TRUE),
    ('Serviços',        '#607D8B', TRUE),
    ('Vestuário',       '#E91E63', TRUE),
    ('Educação',        '#00BCD4', TRUE),
    ('Investimentos',   '#e2b900', TRUE),
    ('Outros',          '#9E9E9E', TRUE);
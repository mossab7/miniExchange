-- V2__update_portfolio_table_fields.sql

ALTER TABLE portfolios ADD COLUMN name VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE portfolios ADD COLUMN description VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE portfolios ADD COLUMN available_balance NUMERIC(38, 0) NOT NULL DEFAULT 0;
ALTER TABLE portfolios ADD COLUMN locked_balance NUMERIC(38, 0) NOT NULL DEFAULT 0;
ALTER TABLE portfolios DROP COLUMN balance;

ALTER TABLE portfolios ADD CONSTRAINT uq_portfolios_user_name UNIQUE (user_id, name);
ALTER TABLE portfolios ADD CONSTRAINT chk_available_balance_non_negative CHECK (available_balance >= 0);
ALTER TABLE portfolios ADD CONSTRAINT chk_locked_balance_non_negative CHECK (locked_balance >= 0);

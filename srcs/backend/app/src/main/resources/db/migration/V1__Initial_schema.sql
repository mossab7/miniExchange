-- V1__Initial_schema.sql
-- Initial database schema for miniExchange application

-- Create sequences
CREATE SEQUENCE user_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE asset_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE portfolio_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE position_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE order_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE balance_transaction_sequence START WITH 1 INCREMENT BY 50;

-- Users table
CREATE TABLE users (
    id BIGINT DEFAULT nextval('user_sequence') PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    locked BOOLEAN DEFAULT false,
    enabled BOOLEAN DEFAULT true,
    role VARCHAR(20) DEFAULT 'USER'
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_uuid ON users(uuid);

-- Assets table
CREATE TABLE assets (
    id BIGINT DEFAULT nextval('asset_sequence') PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    symbol VARCHAR(4) NOT NULL UNIQUE,
    name VARCHAR(20) NOT NULL UNIQUE,
    decimal_places INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_assets_symbol ON assets(symbol);
CREATE INDEX idx_assets_uuid ON assets(uuid);

-- Portfolios table
CREATE TABLE portfolios (
    id BIGINT DEFAULT nextval('portfolio_sequence') PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    balance NUMERIC(38, 0) NOT NULL DEFAULT 0
);

CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX idx_portfolios_uuid ON portfolios(uuid);

-- Positions table
CREATE TABLE positions (
    id BIGINT DEFAULT nextval('position_sequence') PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    asset_id BIGINT NOT NULL REFERENCES assets(id) ON DELETE RESTRICT,
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    quantity NUMERIC(38, 0) NOT NULL,
    UNIQUE(asset_id, portfolio_id)
);

CREATE INDEX idx_positions_asset_id ON positions(asset_id);
CREATE INDEX idx_positions_portfolio_id ON positions(portfolio_id);
CREATE INDEX idx_positions_uuid ON positions(uuid);

-- Orders table
CREATE TABLE orders (
    id BIGINT DEFAULT nextval('order_id_seq') PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    side VARCHAR(20) NOT NULL,
    asset_id BIGINT NOT NULL REFERENCES assets(id) ON DELETE RESTRICT,
    quote_asset_id BIGINT NOT NULL REFERENCES assets(id) ON DELETE RESTRICT,
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    quantity NUMERIC(38, 0) NOT NULL,
    price NUMERIC(38, 0) NOT NULL,
    status VARCHAR(20) NOT NULL,
    filled_quantity NUMERIC(38, 0) NOT NULL,
    remaining_quantity NUMERIC(38, 0) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_orders_asset_id ON orders(asset_id);
CREATE INDEX idx_orders_quote_asset_id ON orders(quote_asset_id);
CREATE INDEX idx_orders_portfolio_id ON orders(portfolio_id);
CREATE INDEX idx_orders_uuid ON orders(uuid);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- Balance Transactions table
CREATE TABLE balance_transactions (
    id BIGINT DEFAULT nextval('balance_transaction_sequence') PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    amount NUMERIC(38, 0) NOT NULL,
    type VARCHAR(50) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE
);

CREATE INDEX idx_balance_transactions_portfolio_id ON balance_transactions(portfolio_id);
CREATE INDEX idx_balance_transactions_uuid ON balance_transactions(uuid);

-- Trades table (minimal for now)
CREATE TABLE trades (
    id BIGINT PRIMARY KEY
);

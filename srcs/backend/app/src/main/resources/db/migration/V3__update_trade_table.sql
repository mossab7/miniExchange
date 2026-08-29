-- V3__update_trade_table.sql

ALTER TABLE trades ADD COLUMN uuid UUID NOT NULL UNIQUE;
ALTER TABLE trades ADD COLUMN buyer_id BIGINT NOT NULL;
ALTER TABLE trades ADD COLUMN seller_id BIGINT NOT NULL;
ALTER TABLE trades ADD COLUMN buyer_order_id BIGINT NOT NULL;
ALTER TABLE trades ADD COLUMN seller_order_id BIGINT NOT NULL;
ALTER TABLE trades ADD COLUMN asset_id BIGINT NOT NULL;
ALTER TABLE trades ADD COLUMN amount NUMERIC(38, 0) NOT NULL;
ALTER TABLE trades ADD COLUMN price NUMERIC(38, 0) NOT NULL;
ALTER TABLE trades ADD COLUMN date TIMESTAMP NOT NULL;

ALTER TABLE trades ADD CONSTRAINT fk_trade_buyer_id FOREIGN KEY (buyer_id) REFERENCES portfolios(id);
ALTER TABLE trades ADD CONSTRAINT fk_trade_seller_id FOREIGN KEY (seller_id) REFERENCES portfolios(id);
ALTER TABLE trades ADD CONSTRAINT fk_trade_buyer_order_id FOREIGN KEY (buyer_order_id) REFERENCES orders(id);
ALTER TABLE trades ADD CONSTRAINT fk_trade_seller_order_id FOREIGN KEY (seller_order_id) REFERENCES orders(id);
ALTER TABLE trades ADD CONSTRAINT fk_trade_asset_id FOREIGN KEY (asset_id) REFERENCES assets(id);

CREATE INDEX idx_trades_uuid ON trades(uuid);
CREATE SEQUENCE trade_sequence START WITH 1 INCREMENT BY 50;

ALTER TABLE trades ALTER COLUMN id SET DEFAULT nextval('trade_sequence');

ALTER TABLE orders DROP COLUMN remaining_quantity;   
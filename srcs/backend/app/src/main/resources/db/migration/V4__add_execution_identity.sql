ALTER TABLE trades ADD COLUMN engine_namespace BIGINT;
ALTER TABLE trades ADD COLUMN execution_id BIGINT;

ALTER TABLE trades ADD CONSTRAINT uq_trades_execution_identity
    UNIQUE (engine_namespace, execution_id);

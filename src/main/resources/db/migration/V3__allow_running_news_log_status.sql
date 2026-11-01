ALTER TABLE news_log
DROP CONSTRAINT chk_news_log_estado;

ALTER TABLE news_log
ADD CONSTRAINT chk_news_log_estado
CHECK (estado IN ('RUNNING', 'SUCCESS', 'FAILED'));
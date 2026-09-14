ALTER TABLE conversation_messages
    ADD COLUMN work_status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    ADD COLUMN work_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN work_started_at TIMESTAMPTZ,
    ADD COLUMN work_token UUID,
    ADD COLUMN reply_type VARCHAR(20),
    ADD COLUMN reply_data JSONB,
    ADD COLUMN error_code VARCHAR(50),
    ADD COLUMN work_updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT conversation_messages_work_status_check
        CHECK (work_status IN ('NEW', 'PROCESSING', 'READY', 'FAILED', 'ERASED')),
    ADD CONSTRAINT conversation_messages_work_attempts_check
        CHECK (work_attempts >= 0),
    ADD CONSTRAINT conversation_messages_processing_check
        CHECK (work_status <> 'PROCESSING' OR (work_started_at IS NOT NULL AND work_token IS NOT NULL)),
    ADD CONSTRAINT conversation_messages_not_processing_check
        CHECK (work_status = 'PROCESSING' OR (work_started_at IS NULL AND work_token IS NULL)),
    ADD CONSTRAINT conversation_messages_ready_check
        CHECK (work_status <> 'READY' OR (reply_type IS NOT NULL AND reply_data IS NOT NULL)),
    ADD CONSTRAINT conversation_messages_not_ready_check
        CHECK (work_status = 'READY' OR (reply_type IS NULL AND reply_data IS NULL)),
    ADD CONSTRAINT conversation_messages_reply_type_check
        CHECK (reply_type IS NULL OR reply_type IN ('TEXT', 'CONFIRMATION')),
    ADD CONSTRAINT conversation_messages_failed_check
        CHECK (work_status <> 'FAILED' OR error_code IS NOT NULL),
    ADD CONSTRAINT conversation_messages_not_failed_check
        CHECK (work_status = 'FAILED' OR error_code IS NULL);

UPDATE conversation_messages
SET work_status = 'ERASED',
    work_updated_at = COALESCE(erased_at, created_at)
WHERE message_text IS NULL OR erased_at IS NOT NULL;

CREATE INDEX conversation_messages_work_idx
    ON conversation_messages (work_status, work_started_at);

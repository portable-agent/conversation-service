ALTER TABLE conversation_messages
    DROP CONSTRAINT conversation_messages_reply_type_check;

ALTER TABLE conversation_messages
    ADD CONSTRAINT conversation_messages_reply_type_check
        CHECK (reply_type IS NULL OR reply_type IN ('TEXT', 'CONFIRMATION', 'CONNECTION'));

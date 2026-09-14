CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    subject VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX conversations_expiry_idx
    ON conversations (status, expires_at);

CREATE TABLE conversation_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    subject VARCHAR(200) NOT NULL,
    request_key VARCHAR(200) NOT NULL,
    message_text VARCHAR(10000),
    locale VARCHAR(16) NOT NULL,
    time_zone VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    erased_at TIMESTAMPTZ,
    CONSTRAINT conversation_messages_request_key_unique
        UNIQUE (tenant_id, subject, request_key)
);

CREATE INDEX conversation_messages_conversation_idx
    ON conversation_messages (conversation_id, created_at);

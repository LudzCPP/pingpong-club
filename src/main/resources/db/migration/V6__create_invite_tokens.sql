CREATE TABLE invite_tokens (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    token       VARCHAR(64) NOT NULL UNIQUE,
    created_by  UUID        NOT NULL REFERENCES users(id),
    expires_at  TIMESTAMP   NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE
);

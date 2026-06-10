CREATE TABLE training_packages (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id           UUID         NOT NULL REFERENCES users(id),
    coach_id            UUID         NOT NULL REFERENCES users(id),
    total_sessions      INTEGER      NOT NULL CHECK (total_sessions > 0),
    remaining_sessions  INTEGER      NOT NULL CHECK (remaining_sessions >= 0),
    price_paid          NUMERIC(8,2) NOT NULL,
    notes               VARCHAR(500),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE join_requests (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    coach_id   UUID        NOT NULL REFERENCES users(id),
    player_id  UUID        NOT NULL REFERENCES users(id),
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_join_request_pending
    ON join_requests (coach_id, player_id)
    WHERE status = 'PENDING';

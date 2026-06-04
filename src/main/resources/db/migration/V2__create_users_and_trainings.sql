-- ============================================================
-- V2: Schemat domenowy – users i trainings
-- ============================================================

CREATE TABLE users (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL CHECK (role IN ('COACH', 'PLAYER')),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);

-- ============================================================

CREATE TABLE trainings (
    id               UUID           NOT NULL DEFAULT gen_random_uuid(),
    -- Zawsze format "trening [Imię]", np. "trening Janusz"
    name             VARCHAR(255)   NOT NULL,
    player_id        UUID           NOT NULL,
    coach_id         UUID           NOT NULL,
    scheduled_at     TIMESTAMP      NOT NULL,
    duration_minutes INT            NOT NULL CHECK (duration_minutes > 0),
    status           VARCHAR(20)    NOT NULL DEFAULT 'SCHEDULED'
                         CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    hourly_rate      NUMERIC(8, 2)  NOT NULL CHECK (hourly_rate >= 0),
    notes            VARCHAR(500),

    CONSTRAINT pk_trainings          PRIMARY KEY (id),
    CONSTRAINT fk_trainings_player   FOREIGN KEY (player_id) REFERENCES users (id),
    CONSTRAINT fk_trainings_coach    FOREIGN KEY (coach_id)  REFERENCES users (id)
);

CREATE INDEX idx_trainings_player_id   ON trainings (player_id);
CREATE INDEX idx_trainings_coach_id    ON trainings (coach_id);
CREATE INDEX idx_trainings_scheduled   ON trainings (scheduled_at);
CREATE INDEX idx_trainings_status      ON trainings (status);

-- Konto startowe trenera tworzone przez DataInitializer (poprawny hash BCrypt)

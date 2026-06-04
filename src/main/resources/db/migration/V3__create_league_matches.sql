-- ============================================================
-- V3: Tabela meczów ligowych
-- ============================================================

CREATE TABLE league_matches (
    id          UUID           NOT NULL DEFAULT gen_random_uuid(),
    player_id   UUID           NOT NULL,
    match_date  DATE           NOT NULL,
    opponent    VARCHAR(200)   NOT NULL,
    result      VARCHAR(10)    NOT NULL,
    payment     NUMERIC(8, 2)  NOT NULL DEFAULT 0 CHECK (payment >= 0),
    notes       VARCHAR(500),

    CONSTRAINT pk_league_matches        PRIMARY KEY (id),
    CONSTRAINT fk_league_matches_player FOREIGN KEY (player_id) REFERENCES users (id)
);

CREATE INDEX idx_league_matches_player_id  ON league_matches (player_id);
CREATE INDEX idx_league_matches_match_date ON league_matches (match_date);

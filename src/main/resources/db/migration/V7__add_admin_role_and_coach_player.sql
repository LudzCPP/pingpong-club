-- Rozszerzenie CHECK constraint o ADMIN przed aktualizacją roli
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'COACH', 'PLAYER'));

-- Migracja roli trener@pingpong.pl na ADMIN
UPDATE users SET role = 'ADMIN' WHERE email = 'trener@pingpong.pl';

-- Relacja trener–zawodnik (many-to-many)
CREATE TABLE coach_player (
    coach_id  UUID NOT NULL REFERENCES users(id),
    player_id UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (coach_id, player_id)
);

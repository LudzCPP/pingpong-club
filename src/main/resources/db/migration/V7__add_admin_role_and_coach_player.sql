-- Migracja roli trener@pingpong.pl na ADMIN
UPDATE users SET role = 'ADMIN' WHERE email = 'trener@pingpong.pl';

-- Relacja trener–zawodnik (many-to-many)
CREATE TABLE coach_player (
    coach_id  UUID NOT NULL REFERENCES users(id),
    player_id UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (coach_id, player_id)
);

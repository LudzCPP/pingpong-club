ALTER TABLE invite_tokens ADD COLUMN virtual_player_id UUID REFERENCES users(id);

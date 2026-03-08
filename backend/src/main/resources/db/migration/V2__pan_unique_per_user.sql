-- Drop global PAN unique constraint, replace with per-user uniqueness
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_pan_key;
ALTER TABLE clients ADD CONSTRAINT uq_clients_user_pan UNIQUE (user_id, pan);

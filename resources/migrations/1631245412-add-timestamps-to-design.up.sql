ALTER table design
    ADD COLUMN created_at TIMESTAMP not null default now(),
    ADD COLUMN updated_at TIMESTAMP not null default now();

ALTER table account
    ADD COLUMN subscription_status text
        NOT NULL DEFAULT 'trialing';

-- liquibase formatted sql
-- changeset training:identity-001-init

CREATE TABLE users (
    id              UUID         PRIMARY KEY,
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(120) NOT NULL,
    status          VARCHAR(32)  NOT NULL
        CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_users_email_ci ON users (LOWER(email));

CREATE TABLE user_roles (
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_code   VARCHAR(64) NOT NULL
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

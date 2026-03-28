ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'CANCELLED';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'ISSUED';

CREATE TABLE app_users
(
    id                bigserial primary key,
    username          varchar(128) not null unique,
    password          varchar(255) not null,
    pickup_point_id   bigint references pickup_points (id)
);

CREATE TABLE app_user_roles
(
    user_id   bigint       not null references app_users (id) on delete cascade,
    role_name varchar(64) not null,
    primary key (user_id, role_name)
);

-- password for all test users: "password" (BCrypt)
INSERT INTO app_users (username, password, pickup_point_id)
VALUES ('user1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NULL),
       ('ppadmin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 1),
       ('superadmin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NULL);

INSERT INTO app_user_roles (user_id, role_name)
VALUES ((SELECT id FROM app_users WHERE username = 'user1'), 'USER'),
       ((SELECT id FROM app_users WHERE username = 'ppadmin'), 'PICKUP_POINT_ADMIN'),
       ((SELECT id FROM app_users WHERE username = 'superadmin'), 'ADMIN');

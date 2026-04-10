CREATE TABLE network_policies
(
    id          bigserial primary key,
    name        varchar(100) not null unique,
    description text
);

CREATE TABLE network_policy_cidrs
(
    id          bigserial primary key,
    policy_id   bigint       not null references network_policies (id) on delete cascade,
    cidr        varchar(43)  not null,
    description text
);

CREATE TABLE network_policy_roles
(
    policy_id bigint       not null references network_policies (id) on delete cascade,
    role_name varchar(64)  not null,
    primary key (policy_id, role_name)
);

-- Пример из задания: A=USER, B=PICKUP_POINT_ADMIN, V=ADMIN (условно)
-- Политика 1: 0.0.0.0/0 -> все три роли
INSERT INTO network_policies (id, name, description)
VALUES (1, 'global-ipv4', 'Любой IPv4 (наименьший приоритет маски)');

INSERT INTO network_policy_cidrs (policy_id, cidr, description)
VALUES (1, '0.0.0.0/0', 'весь IPv4');

INSERT INTO network_policy_roles (policy_id, role_name)
VALUES (1, 'USER'),
       (1, 'PICKUP_POINT_ADMIN'),
       (1, 'ADMIN');

-- Политика 2: 173.0.0.0/8 -> USER, PICKUP_POINT_ADMIN
INSERT INTO network_policies (id, name, description)
VALUES (2, '173-net', 'Сеть 173.0.0.0/8');

INSERT INTO network_policy_cidrs (policy_id, cidr, description)
VALUES (2, '173.0.0.0/8', 'пример /8');

INSERT INTO network_policy_roles (policy_id, role_name)
VALUES (2, 'USER'),
       (2, 'PICKUP_POINT_ADMIN');

-- Политика 3: 173.0.0.0/24 -> только USER
INSERT INTO network_policies (id, name, description)
VALUES (3, '173-zero-slash24', 'Подсеть 173.0.0.0/24');

INSERT INTO network_policy_cidrs (policy_id, cidr, description)
VALUES (3, '173.0.0.0/24', 'пример /24');

INSERT INTO network_policy_roles (policy_id, role_name)
VALUES (3, 'USER');

-- Доп. пользователь для демонстрации сценариев A,B,V (все три роли)
INSERT INTO app_users (username, password, pickup_point_id)
VALUES ('netdemo', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', NULL);

INSERT INTO app_user_roles (user_id, role_name)
VALUES ((SELECT id FROM app_users WHERE username = 'netdemo'), 'USER'),
       ((SELECT id FROM app_users WHERE username = 'netdemo'), 'PICKUP_POINT_ADMIN'),
       ((SELECT id FROM app_users WHERE username = 'netdemo'), 'ADMIN');

SELECT setval(
        pg_get_serial_sequence('network_policies', 'id'),
        COALESCE((SELECT MAX(id) FROM network_policies), 1)
       );

create type payment_status as enum ('PENDING', 'COMPLETED', 'FAILED');

create table payments
(
    id             uuid primary key,
    amount_kopecks bigint         not null,
    status         payment_status not null
);

create table card_info
(
    id              bigserial primary key,
    card_id         varchar(17) not null,
    expire_date     varchar(5)  not null,
    cvc             varchar(3)  not null,
    balance_kopecks bigint       not null
);

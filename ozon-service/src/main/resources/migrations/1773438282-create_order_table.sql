create type order_status as enum ('NEW', 'PAID', 'PAYMENT_ERROR');

create table orders
(
    id bigserial primary key,
    payment_id uuid,
    order_status order_status,
    username text,
    pickup_point_id bigint not null references pickup_points(id),
    delivery_address text
)
CREATE TABLE pickup_points
(
    id      BIGSERIAL PRIMARY KEY,
    address TEXT          NOT NULL,
    city    TEXT          NOT NULL,
    lat     DECIMAL(9, 6) NOT NULL,
    lng     DECIMAL(9, 6) NOT NULL
);



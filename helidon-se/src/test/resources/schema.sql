CREATE TABLE fruits (
    id bigint NOT NULL,
    description varchar(255),
    name varchar(255) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE SEQUENCE fruits_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE stores (
    id bigint NOT NULL,
    address varchar(255) NOT NULL,
    city varchar(255) NOT NULL,
    country varchar(255) NOT NULL,
    currency varchar(255) NOT NULL,
    name varchar(255) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE SEQUENCE stores_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE store_fruit_prices (
    price numeric(12, 2) NOT NULL,
    fruit_id bigint NOT NULL,
    store_id bigint NOT NULL,
    PRIMARY KEY (fruit_id, store_id),
    CONSTRAINT fruit_id_fk FOREIGN KEY (fruit_id) REFERENCES fruits,
    CONSTRAINT store_id_fk FOREIGN KEY (store_id) REFERENCES stores
);

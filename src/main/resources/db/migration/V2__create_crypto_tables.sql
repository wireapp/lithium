CREATE TABLE Sessions (
    id         varchar NOT NULL,
    sid        varchar NOT NULL,
    data       bytea NOT NULL,
    PRIMARY KEY (id, sid)
);

CREATE TABLE Identities (
    id         varchar PRIMARY KEY,
    data       bytea NOT NULL
);

CREATE TABLE Prekeys (
    id         varchar NOT NULL,
    kid        integer NOT NULL,
    data       bytea NOT NULL,
    PRIMARY KEY (id, kid)
);

CREATE TABLE Countries (
    country_id VARCHAR(3) NOT NULL,
    country_name_En VARCHAR(32) NOT NULL,
    is_schengen BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (country_id)
);


CREATE TABLE Airlines (
    airline_id VARCHAR(3) PRIMARY KEY,
    airline_name VARCHAR(64) NOT NULL,
    country_id VARCHAR(3) NOT NULL,
    FOREIGN KEY (country_id) REFERENCES Countries(country_id)
);

CREATE TABLE Airports (
    iata_code    CHAR(3) PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    country      VARCHAR(100) NOT NULL,
    country_code CHAR(2) NOT NULL,
    is_schengen  BOOLEAN NOT NULL
);


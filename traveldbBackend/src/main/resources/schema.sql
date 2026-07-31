CREATE TABLE Countries (
    source_id BIGINT NOT NULL UNIQUE,
    country_id CHAR(2) NOT NULL,
    country_name_En VARCHAR(100) NOT NULL,
    continent CHAR(2),
    wikipedia_url VARCHAR(1000),
    keywords VARCHAR(2000),
    is_schengen BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (country_id)
);
CREATE TABLE Airports (
    source_id        BIGINT NOT NULL UNIQUE,
    ident            VARCHAR(32) NOT NULL,
    iata_code        CHAR(3) PRIMARY KEY,
    icao_code        VARCHAR(8),
    gps_code         VARCHAR(16),
    local_code       VARCHAR(16),
    name             VARCHAR(200) NOT NULL,
    municipality     VARCHAR(160),
    region_code      VARCHAR(16),
    country          VARCHAR(100) NOT NULL,
    country_code     CHAR(2) NOT NULL,
    continent        CHAR(2),
    airport_type     VARCHAR(32) NOT NULL,
    scheduled_service BOOLEAN NOT NULL DEFAULT FALSE,
    latitude_deg     DOUBLE PRECISION NOT NULL,
    longitude_deg    DOUBLE PRECISION NOT NULL,
    elevation_ft     INTEGER,
    official_url     VARCHAR(1000),
    wikipedia_url    VARCHAR(1000),
    keywords         VARCHAR(4000),
    is_schengen      BOOLEAN NOT NULL,
    FOREIGN KEY (country_code) REFERENCES Countries(country_id)
);


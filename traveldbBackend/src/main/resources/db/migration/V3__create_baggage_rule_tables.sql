CREATE TABLE baggage_rule_datasets (
    dataset_version VARCHAR(100) PRIMARY KEY,
    reviewed_date DATE NOT NULL,
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE active_baggage_rule_dataset (
    slot SMALLINT PRIMARY KEY,
    dataset_version VARCHAR(100) NOT NULL UNIQUE,
    CONSTRAINT chk_active_baggage_dataset_slot CHECK (slot = 1),
    FOREIGN KEY (dataset_version) REFERENCES baggage_rule_datasets(dataset_version)
);

CREATE TABLE baggage_sources (
    dataset_version VARCHAR(100) NOT NULL,
    source_key VARCHAR(100) NOT NULL,
    label VARCHAR(300) NOT NULL,
    url VARCHAR(2000) NOT NULL,
    PRIMARY KEY (dataset_version, source_key),
    FOREIGN KEY (dataset_version) REFERENCES baggage_rule_datasets(dataset_version) ON DELETE CASCADE
);

CREATE TABLE baggage_airport_group_members (
    dataset_version VARCHAR(100) NOT NULL,
    group_code VARCHAR(100) NOT NULL,
    airport_code CHAR(3) NOT NULL,
    PRIMARY KEY (dataset_version, group_code, airport_code),
    FOREIGN KEY (dataset_version) REFERENCES baggage_rule_datasets(dataset_version) ON DELETE CASCADE
);

CREATE TABLE baggage_rules (
    dataset_version VARCHAR(100) NOT NULL,
    rule_id VARCHAR(150) NOT NULL,
    rule_position INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    entering_country BOOLEAN,
    onward_domestic BOOLEAN,
    current_country_code CHAR(2),
    current_airport_code CHAR(3),
    previous_airport_code CHAR(3),
    previous_airport_group VARCHAR(100),
    ticket_arrangement VARCHAR(40),
    through_check_status VARCHAR(20),
    advice_code VARCHAR(100) NOT NULL,
    advice_status VARCHAR(30) NOT NULL,
    title VARCHAR(500) NOT NULL,
    explanation VARCHAR(4000) NOT NULL,
    PRIMARY KEY (dataset_version, rule_id),
    UNIQUE (dataset_version, rule_position),
    FOREIGN KEY (dataset_version) REFERENCES baggage_rule_datasets(dataset_version) ON DELETE CASCADE,
    CONSTRAINT chk_baggage_rule_priority CHECK (priority >= 0)
);

CREATE TABLE baggage_rule_exceptions (
    dataset_version VARCHAR(100) NOT NULL,
    rule_id VARCHAR(150) NOT NULL,
    exception_position INTEGER NOT NULL,
    exception_text VARCHAR(2000) NOT NULL,
    PRIMARY KEY (dataset_version, rule_id, exception_position),
    FOREIGN KEY (dataset_version, rule_id)
        REFERENCES baggage_rules(dataset_version, rule_id) ON DELETE CASCADE
);

CREATE TABLE baggage_rule_sources (
    dataset_version VARCHAR(100) NOT NULL,
    rule_id VARCHAR(150) NOT NULL,
    source_position INTEGER NOT NULL,
    source_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (dataset_version, rule_id, source_position),
    FOREIGN KEY (dataset_version, rule_id)
        REFERENCES baggage_rules(dataset_version, rule_id) ON DELETE CASCADE,
    FOREIGN KEY (dataset_version, source_key)
        REFERENCES baggage_sources(dataset_version, source_key)
);

CREATE INDEX idx_baggage_rules_priority ON baggage_rules(dataset_version, priority, rule_position);
CREATE INDEX idx_baggage_airport_groups
    ON baggage_airport_group_members(dataset_version, group_code, airport_code);

# --- !Ups

CREATE TABLE Organization (
	org_id VARCHAR(100) PRIMARY KEY,
  description VARCHAR(255) NOT NULL,
	password VARCHAR(255) NOT NULL,
	salt VARCHAR(255) NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS Organization;

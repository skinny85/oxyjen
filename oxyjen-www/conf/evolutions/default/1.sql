# --- !Ups

CREATE TABLE Organization (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	org_id VARCHAR(100) NOT NULL UNIQUE,
	password VARCHAR(255),
	salt VARCHAR(255)
);

# --- !Downs

DROP TABLE IF EXISTS Organization;

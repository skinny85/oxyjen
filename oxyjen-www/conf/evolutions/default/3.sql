# --- !Ups

CREATE TABLE Session (
	id VARCHAR(255) PRIMARY KEY,
	org_id VARCHAR(100) NOT NULL,
	active BOOLEAN NOT NULL,
	created DATE NOT NULL,
	expires DATE NOT NULL,
	ip_address VARCHAR(64),

	FOREIGN KEY (org_id) REFERENCES Organization(org_id)
);

# --- !Downs

DROP TABLE IF EXISTS Session;

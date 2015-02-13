# --- !Ups

INSERT INTO Organization (org_id, description, password, salt) VALUES
  ('oxyjen', '', 'password', 'salt');

# --- !Downs

DELETE FROM Organization WHERE org_id = 'oxyjen';

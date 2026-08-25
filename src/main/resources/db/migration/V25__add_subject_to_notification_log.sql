ALTER TABLE notification_log ADD COLUMN subject VARCHAR(255) NOT NULL DEFAULT 'Notification';

ALTER TABLE notification_log ALTER COLUMN subject DROP DEFAULT;

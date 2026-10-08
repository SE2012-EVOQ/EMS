-- Additive, repeatable migration; never resets a completed installation.
-- Apply after 01_schema table DDL and 06_runtime_roles, using a migration account.
CREATE TABLE IF NOT EXISTS first_run_setup (
    setup_id TINYINT NOT NULL PRIMARY KEY,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at DATETIME NULL,
    CONSTRAINT chk_first_run_singleton CHECK (setup_id = 1)
) ENGINE=InnoDB;

INSERT IGNORE INTO first_run_setup (setup_id, completed, completed_at)
SELECT 1, EXISTS (SELECT 1 FROM user_account),
       CASE WHEN EXISTS (SELECT 1 FROM user_account) THEN CURRENT_TIMESTAMP ELSE NULL END;

-- Existing accounts, including inactive/non-admin accounts, mean this is not
-- a new installation. Never reopen setup just because an admin is unavailable.
UPDATE first_run_setup
SET completed = TRUE, completed_at = COALESCE(completed_at, CURRENT_TIMESTAMP)
WHERE setup_id = 1 AND completed = FALSE AND EXISTS (SELECT 1 FROM user_account);

-- Migration: add submit column to task_submissions
-- SQLite does not support DROP COLUMN easily, but adding a column is straightforward.
-- Run this once after deploying the new code.
-- Existing rows will have NULL; we update them to 0 (false).

ALTER TABLE task_submissions ADD COLUMN submit INTEGER DEFAULT 0;

-- Normalize any NULL values to 0 (if the column was created without default for existing rows)
UPDATE task_submissions SET submit = 0 WHERE submit IS NULL;

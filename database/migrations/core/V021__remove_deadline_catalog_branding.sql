DELETE FROM plan_limits
WHERE resource = 'deadlines';

UPDATE plans
SET description = 'For trying opensources with a small team.',
    updated_at = CURRENT_TIMESTAMP
WHERE key = 'free'
  AND description = 'For trying Deadlines with a small team.';

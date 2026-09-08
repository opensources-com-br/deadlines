UPDATE plans
SET is_active = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE key IN ('pro', 'business')
  AND is_active = TRUE;

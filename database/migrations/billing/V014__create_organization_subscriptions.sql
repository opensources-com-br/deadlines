CREATE TABLE organization_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES plans(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'canceled')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT organization_subscriptions_dates CHECK (
        (status = 'active' AND ended_at IS NULL) OR
        (status = 'canceled' AND ended_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX organization_subscriptions_one_active_per_organization
    ON organization_subscriptions (organization_id)
    WHERE status = 'active';

INSERT INTO organization_subscriptions (organization_id, plan_id, status)
SELECT organizations.id, plans.id, 'active'
FROM organizations
CROSS JOIN plans
WHERE plans.key = 'free'
  AND plans.is_active = TRUE;

CREATE FUNCTION provision_free_organization_subscription()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO organization_subscriptions (organization_id, plan_id, status)
    SELECT NEW.id, plans.id, 'active'
    FROM plans
    WHERE plans.key = 'free'
      AND plans.is_active = TRUE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'An active free plan is required to create an organization';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER organizations_provision_free_subscription
AFTER INSERT ON organizations
FOR EACH ROW
EXECUTE FUNCTION provision_free_organization_subscription();

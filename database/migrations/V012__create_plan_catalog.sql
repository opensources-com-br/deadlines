CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    monthly_price_cents INTEGER NOT NULL DEFAULT 0 CHECK (monthly_price_cents >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'USD' CHECK (currency ~ '^[A-Z]{3}$'),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT plans_key_format CHECK (key ~ '^[a-z][a-z0-9_]*$')
);

CREATE TABLE plan_limits (
    plan_id UUID NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    resource VARCHAR(80) NOT NULL,
    limit_value INTEGER NOT NULL CHECK (limit_value >= -1),
    PRIMARY KEY (plan_id, resource),
    CONSTRAINT plan_limits_resource_format CHECK (resource ~ '^[a-z][a-z0-9_]*$')
);

CREATE INDEX plans_public_catalog_idx ON plans (is_active, display_order, name);

INSERT INTO plans (key, name, description, monthly_price_cents, currency, display_order)
VALUES
    ('free', 'Free', 'For trying Deadlines with a small team.', 0, 'USD', 10),
    ('pro', 'Pro', 'For growing teams that need more capacity.', 2900, 'USD', 20),
    ('business', 'Business', 'For organizations that need unlimited collaboration.', 9900, 'USD', 30);

INSERT INTO plan_limits (plan_id, resource, limit_value)
SELECT id, resource, limit_value
FROM plans
CROSS JOIN LATERAL (
    VALUES
        ('free', 'members', 3), ('free', 'projects', 3), ('free', 'deadlines', 50),
        ('pro', 'members', 25), ('pro', 'projects', 50), ('pro', 'deadlines', 1000),
        ('business', 'members', -1), ('business', 'projects', -1), ('business', 'deadlines', -1)
) AS limits(plan_key, resource, limit_value)
WHERE plans.key = limits.plan_key;

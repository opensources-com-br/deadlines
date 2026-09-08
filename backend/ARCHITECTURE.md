# Reusable application boundaries

The backend is composed from a generic core, optional modules, and a product composition root.

```text
core
├── identity          users, authentication, sessions, and preferences
├── organizations     organizations, memberships, authorization, and audit
├── errors            API error contract
└── observability     request correlation, logging, and health checks

modules
└── billing           plans and subscriptions

product
└── deadlines         runtime composition in application/
```

The current source folders retain their established names while the composition boundary is introduced incrementally. `identity`, `organizations`, and `shared` are core code; `modules/billing` is optional; `application` is the product composition root.

Dependency rules:

- Core code must not import `modules` or product code.
- Optional modules may depend on core interfaces.
- The product composition root selects and wires optional modules.

## Optional billing

Billing is enabled by default. Set `FEATURE_BILLING_ENABLED=false` to omit plan and subscription services and their API routes. The web client must also receive `NEXT_PUBLIC_FEATURE_BILLING_ENABLED=false` so it hides billing navigation and pages.

Organization creation, identity, membership, and authorization do not require the billing module. This permits a new product to use the core foundation without importing the billing composition.

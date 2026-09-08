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
└── application       runtime composition root
```

`identity`, `organizations`, and `shared` are core code; `modules/billing` is optional; `application` is the product composition root.

## Product configuration

`ProductConfig` is the single runtime contract for product identity and optional modules. It provides the product name, description, logo, locale, timezone, support email, application URL, and enabled-module set.

The corresponding environment variables use the `PRODUCT_` prefix. `PRODUCT_ENABLED_MODULES` is a comma-separated list; for example, `PRODUCT_ENABLED_MODULES=billing`. When it is not supplied, the legacy `FEATURE_BILLING_ENABLED` flag determines the default during the transition.

Dependency rules:

- Core code must not import `modules` or product code.
- Optional modules may depend on core interfaces.
- The product composition root selects and wires optional modules.

## Optional billing

Billing is enabled when `billing` is present in `PRODUCT_ENABLED_MODULES`. During the transition, `FEATURE_BILLING_ENABLED=false` removes it from the default backend module set. The web client must also receive `NEXT_PUBLIC_FEATURE_BILLING_ENABLED=false` so it hides billing navigation and pages.

Organization creation, identity, membership, and authorization do not require the billing module. This permits a new product to use the core foundation without importing the billing composition.

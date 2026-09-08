# Extending opensources

## Configure a product

Set the `PRODUCT_` variables in `.env` to define the backend product identity and mirror them as `NEXT_PUBLIC_PRODUCT_` variables for the web app. The enabled modules list is comma separated. A product with no optional module can use `PRODUCT_ENABLED_MODULES=core` and `NEXT_PUBLIC_PRODUCT_ENABLED_MODULES=core`.

## Add a module

Place module code under `backend/src/main/kotlin/opensources/modules/<module>`. The module exposes its services and route registration, while `application` is the only composition root that enables it. Put schema changes in `database/migrations/<module>` and add that location only when the module is enabled. Web code for an optional module must be hidden behind `isModuleEnabled` from `web/lib/product-config.ts`.

## Add a permission

Add a stable `<area>.<action>` key to the core or owning module's migration. Authorize server-side routes and services with that key, then use `Can` in the web interface only as a visibility guard. Client guards never replace backend authorization.

## Add a migration

Choose `database/migrations/core` for required schema or the owning module directory for optional schema. Use the next unused Flyway version across all directories, never change an applied migration, and validate with `cd backend && ./gradlew test`.

const legacyBillingEnabled = process.env.NEXT_PUBLIC_FEATURE_BILLING_ENABLED !== "false";

function configuredModules(): ReadonlySet<string> {
  const configured = process.env.NEXT_PUBLIC_PRODUCT_ENABLED_MODULES;
  if (!configured) return new Set(legacyBillingEnabled ? ["billing"] : []);
  return new Set(configured.split(",").map((value) => value.trim()).filter(Boolean));
}

export const productConfig = {
  name: process.env.NEXT_PUBLIC_PRODUCT_NAME?.trim() || "opensources",
  description: process.env.NEXT_PUBLIC_PRODUCT_DESCRIPTION?.trim() || "A reusable SaaS foundation",
  logo: process.env.NEXT_PUBLIC_PRODUCT_LOGO?.trim() || "/opensources-mark.png",
  defaultLocale: process.env.NEXT_PUBLIC_PRODUCT_DEFAULT_LOCALE?.trim() || "en",
  defaultTimezone: process.env.NEXT_PUBLIC_PRODUCT_DEFAULT_TIMEZONE?.trim() || "UTC",
  supportEmail: process.env.NEXT_PUBLIC_PRODUCT_SUPPORT_EMAIL?.trim() || "support@opensources.local",
  applicationUrl: process.env.NEXT_PUBLIC_PRODUCT_APPLICATION_URL?.trim() || "http://localhost:3000",
  enabledModules: configuredModules(),
} as const;

export function isModuleEnabled(moduleName: string): boolean {
  return productConfig.enabledModules.has(moduleName);
}

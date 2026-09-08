import { describe, expect, it } from "vitest";

import { isModuleEnabled, productConfig } from "@/lib/product-config";

describe("productConfig", () => {
  it("provides generic defaults for a new product", () => {
    expect(productConfig.name).toBe("opensources");
    expect(productConfig.defaultLocale).toBe("en");
    expect(productConfig.defaultTimezone).toBe("UTC");
  });

  it("uses enabled modules as the source of feature flags", () => {
    expect(isModuleEnabled("billing")).toBe(true);
    expect(isModuleEnabled("unknown-module")).toBe(false);
  });
});

import { afterEach, describe, expect, it, vi } from "vitest";

afterEach(() => {
  vi.unstubAllEnvs();
  vi.resetModules();
});

describe("optional modules", () => {
  it("does not expose billing when it is not enabled", async () => {
    vi.stubEnv("NEXT_PUBLIC_PRODUCT_ENABLED_MODULES", "reports");
    vi.resetModules();

    const { billingEnabled } = await import("@/lib/features");
    expect(billingEnabled).toBe(false);
  });

  it("exposes billing only when the product enables it", async () => {
    vi.stubEnv("NEXT_PUBLIC_PRODUCT_ENABLED_MODULES", "billing,reports");
    vi.resetModules();

    const { billingEnabled } = await import("@/lib/features");
    expect(billingEnabled).toBe(true);
  });
});

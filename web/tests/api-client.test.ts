import { afterEach, describe, expect, it, vi } from "vitest";

import { apiClient } from "@/lib/api-client";

describe("apiClient", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("includes the CSRF token on browser mutations", async () => {
    document.cookie = "opensources_csrf_token=token-value; path=/";
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ ok: true }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    await apiClient.post("/api/example", { value: true });

    const [, options] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(new Headers(options.headers).get("X-CSRF-Token")).toBe("token-value");
  });
});

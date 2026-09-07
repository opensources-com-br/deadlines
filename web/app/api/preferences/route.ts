import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import type { UserPreference } from "@/features/platform/domain/user-preference";
import { isAppLocale, localeCookieName } from "@/i18n/config";

async function forward(method: "GET" | "PATCH", body?: unknown) {
  const accessToken = (await cookies()).get("deadlines_access_token")?.value;
  if (!accessToken) return NextResponse.json({ error: { code: "UNAUTHORIZED", message: "Authentication is required." } }, { status: 401 });
  try {
    const backendResponse = await fetch(backendApiUrl("/api/v1/users/me/preferences"), {
      method,
      headers: { Authorization: `Bearer ${accessToken}`, ...(body ? { "Content-Type": "application/json" } : {}) },
      body: body ? JSON.stringify(body) : undefined,
      cache: "no-store",
    });
    const data = await backendResponse.json().catch(() => null);
    const response = NextResponse.json(data, { status: backendResponse.status });
    if (backendResponse.ok && data) {
      const preferences = data as UserPreference;
      const cookieOptions = { httpOnly: true, sameSite: "lax" as const, secure: process.env.NODE_ENV === "production", path: "/", maxAge: 60 * 60 * 24 * 365 };
      if (isAppLocale(preferences.locale)) response.cookies.set(localeCookieName, preferences.locale, cookieOptions);
      response.cookies.set("deadlines_timezone", preferences.timezone, cookieOptions);
      response.cookies.set("deadlines_theme", preferences.theme, cookieOptions);
    }
    return response;
  } catch {
    return NextResponse.json({ error: { code: "BACKEND_UNAVAILABLE", message: "Preferences service is unavailable." } }, { status: 503 });
  }
}

export function GET() { return forward("GET"); }
export async function PATCH(request: Request) { return forward("PATCH", await request.json().catch(() => null)); }

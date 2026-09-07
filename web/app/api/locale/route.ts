import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { isAppLocale, localeCookieName } from "@/i18n/config";
import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

export async function PUT(request: Request) {
  const payload = await request.json().catch(() => null) as { locale?: unknown } | null;
  if (!isAppLocale(payload?.locale)) {
    return NextResponse.json(
      { error: { code: "INVALID_LOCALE", message: "The selected language is not supported." } },
      { status: 422 },
    );
  }

  const accessToken = (await cookies()).get("deadlines_access_token")?.value;
  if (!accessToken) {
    return NextResponse.json({ error: { code: "UNAUTHORIZED", message: "Authentication is required." } }, { status: 401 });
  }

  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendApiUrl("/api/v1/users/me/preferences"), {
      method: "PATCH",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({ locale: payload.locale }),
      cache: "no-store",
    });
  } catch {
    return NextResponse.json({ error: { code: "BACKEND_UNAVAILABLE", message: "Preferences service is unavailable." } }, { status: 503 });
  }
  if (!backendResponse.ok) {
    const data = await backendResponse.json().catch(() => null);
    return NextResponse.json(data ?? { error: { code: "PREFERENCE_UPDATE_FAILED", message: "Unable to update preferences." } }, { status: backendResponse.status });
  }

  const response = new NextResponse(null, { status: 204 });
  response.cookies.set(localeCookieName, payload.locale, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60 * 24 * 365,
  });
  return response;
}

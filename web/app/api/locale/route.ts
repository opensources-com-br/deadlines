import { NextResponse } from "next/server";

import { isAppLocale, localeCookieName } from "@/i18n/config";

export async function PUT(request: Request) {
  const payload = await request.json().catch(() => null) as { locale?: unknown } | null;
  if (!isAppLocale(payload?.locale)) {
    return NextResponse.json(
      { error: { code: "INVALID_LOCALE", message: "The selected language is not supported." } },
      { status: 422 },
    );
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

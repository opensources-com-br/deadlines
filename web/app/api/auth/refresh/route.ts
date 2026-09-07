import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

const accessCookieName = "deadlines_access_token";
const refreshCookieName = "deadlines_refresh_token";

type RefreshResponse = { accessToken: string; refreshToken: string; expiresIn: number };

function safeReturnTo(value: string | null) {
  return value?.startsWith("/app") ? value : "/app";
}

function clearSession(response: NextResponse) {
  response.cookies.delete(accessCookieName);
  response.cookies.delete(refreshCookieName);
  return response;
}

function setSessionCookies(response: NextResponse, auth: RefreshResponse) {
  const secure = process.env.NODE_ENV === "production";
  response.cookies.set(accessCookieName, auth.accessToken, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: auth.expiresIn });
  response.cookies.set(refreshCookieName, auth.refreshToken, { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 60 * 60 * 24 * 30 });
  return response;
}

async function refreshSession() {
  const refreshToken = (await cookies()).get(refreshCookieName)?.value;
  if (!refreshToken) return { status: 401, auth: undefined };

  try {
    const backendResponse = await fetch(backendApiUrl("/api/v1/auth/refresh"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    });
    const auth = await backendResponse.json().catch(() => undefined) as RefreshResponse | undefined;
    return { status: backendResponse.status, auth: backendResponse.ok ? auth : undefined };
  } catch {
    return { status: 503, auth: undefined };
  }
}

export async function POST() {
  const { status, auth } = await refreshSession();
  if (!auth) {
    const response = NextResponse.json({ error: { code: "SESSION_REFRESH_FAILED", message: status === 503 ? "Unable to refresh your session right now." : "Your session has ended. Please sign in again." } }, { status });
    return status === 401 ? clearSession(response) : response;
  }
  return setSessionCookies(NextResponse.json({ expiresIn: auth.expiresIn }), auth);
}

export async function GET(request: Request) {
  const { status, auth } = await refreshSession();
  if (!auth) {
    if (status === 401) return clearSession(NextResponse.redirect(new URL("/login", request.url), 302));
    return NextResponse.json({ error: { code: "SESSION_REFRESH_UNAVAILABLE", message: "Unable to refresh your session right now." } }, { status: 503 });
  }
  return setSessionCookies(NextResponse.redirect(new URL(safeReturnTo(new URL(request.url).searchParams.get("returnTo")), request.url)), auth);
}

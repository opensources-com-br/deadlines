import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import { deviceCookieName, newDeviceId, setDeviceCookie } from "@/features/identity/infrastructure/device-session";
import { authCookies, persistentCookieOptions, secureCookieOptions } from "@/lib/cookies";

const accessCookieName = authCookies.accessToken;
const refreshCookieName = authCookies.refreshToken;
const activityCookieName = authCookies.lastActivity;
const persistentCookieName = authCookies.persistentSession;

type RefreshResponse = { accessToken: string; refreshToken: string; expiresIn: number };

function safeReturnTo(value: string | null) {
  return value?.startsWith("/app") ? value : "/app";
}

function clearSession(response: NextResponse) {
  response.cookies.delete(accessCookieName);
  response.cookies.delete(refreshCookieName);
  response.cookies.delete(activityCookieName);
  response.cookies.delete(persistentCookieName);
  return response;
}

function setSessionCookies(response: NextResponse, auth: RefreshResponse, persistentSession: boolean) {
  response.cookies.set(accessCookieName, auth.accessToken, persistentCookieOptions(auth.expiresIn));
  response.cookies.set(refreshCookieName, auth.refreshToken, persistentSession ? persistentCookieOptions(60 * 60 * 24 * 30) : secureCookieOptions);
  response.cookies.set(activityCookieName, "active", persistentCookieOptions(auth.expiresIn));
  if (persistentSession) response.cookies.set(persistentCookieName, "true", persistentCookieOptions(60 * 60 * 24 * 30));
  return response;
}

async function refreshSession() {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(refreshCookieName)?.value;
  const deviceId = cookieStore.get(deviceCookieName)?.value ?? newDeviceId();
  const persistentSession = cookieStore.get(persistentCookieName)?.value === "true";
  if (!refreshToken) return { status: 401, auth: undefined, persistentSession };

  try {
    const backendResponse = await fetch(backendApiUrl("/api/v1/auth/refresh"), {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Device-Id": deviceId },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    });
    const auth = await backendResponse.json().catch(() => undefined) as RefreshResponse | undefined;
    return { status: backendResponse.status, auth: backendResponse.ok ? auth : undefined, persistentSession, deviceId };
  } catch {
    return { status: 503, auth: undefined, persistentSession, deviceId };
  }
}

export async function POST() {
  const { status, auth, persistentSession, deviceId } = await refreshSession();
  if (!auth) {
    const response = NextResponse.json({ error: { code: "SESSION_REFRESH_FAILED", message: status === 503 ? "Unable to refresh your session right now." : "Your session has ended. Please sign in again." } }, { status });
    return status === 401 ? clearSession(response) : response;
  }
  const response = setSessionCookies(NextResponse.json({ expiresIn: auth.expiresIn }), auth, persistentSession);
  setDeviceCookie(response, deviceId);
  return response;
}

export async function GET(request: Request) {
  const { status, auth, persistentSession, deviceId } = await refreshSession();
  if (!auth) {
    if (status === 401) return clearSession(NextResponse.redirect(new URL("/login", request.url), 302));
    return NextResponse.json({ error: { code: "SESSION_REFRESH_UNAVAILABLE", message: "Unable to refresh your session right now." } }, { status: 503 });
  }
  const response = setSessionCookies(NextResponse.redirect(new URL(safeReturnTo(new URL(request.url).searchParams.get("returnTo")), request.url)), auth, persistentSession);
  setDeviceCookie(response, deviceId);
  return response;
}

import { NextRequest, NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import { deviceCookieName, newDeviceId, setDeviceCookie } from "@/features/identity/infrastructure/device-session";
import { isAppLocale, localeCookieName } from "@/i18n/config";
import { authCookies, clearLegacyCookies, persistentCookieOptions, preferenceCookies, secureCookieOptions } from "@/lib/cookies";

const accessCookieName = authCookies.accessToken;
const refreshCookieName = authCookies.refreshToken;
const activityCookieName = authCookies.lastActivity;
const persistentCookieName = authCookies.persistentSession;

type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    profile: {
      firstName: string;
      lastName: string;
    };
  };
};

export async function POST(request: NextRequest) {
  const payload = await request.json().catch(() => null) as { email?: string; password?: string; keepSignedIn?: boolean } | null;
  if (!payload) {
    return NextResponse.json(
      { error: { code: "INVALID_REQUEST", message: "Request body is invalid" } },
      { status: 400 },
    );
  }

  const deviceId = request.cookies.get(deviceCookieName)?.value ?? newDeviceId();
  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendApiUrl("/api/v1/auth/login"), {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Device-Id": deviceId },
      body: JSON.stringify({ email: payload.email, password: payload.password }),
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { error: { code: "BACKEND_UNAVAILABLE", message: "Authentication service is unavailable" } },
      { status: 503 },
    );
  }

  const data = await backendResponse.json().catch(() => null);
  if (!backendResponse.ok) {
    return NextResponse.json(data ?? { error: { code: "AUTHENTICATION_FAILED", message: "Unable to sign in" } }, {
      status: backendResponse.status,
    });
  }

  const auth = data as AuthResponse;
  const preferences = await fetch(backendApiUrl("/api/v1/users/me/preferences"), {
    headers: { Authorization: `Bearer ${auth.accessToken}` },
    cache: "no-store",
  }).then((result) => result.ok ? result.json() as Promise<{ locale?: unknown; timezone?: string; theme?: string }> : undefined).catch(() => undefined);
  const response = NextResponse.json({ user: auth.user });
  clearLegacyCookies(response);
  const keepSignedIn = payload.keepSignedIn === true;
  setDeviceCookie(response, deviceId);
  if (isAppLocale(preferences?.locale)) {
    response.cookies.set(localeCookieName, preferences.locale, persistentCookieOptions(60 * 60 * 24 * 365));
  }
  const preferenceCookieOptions = persistentCookieOptions(60 * 60 * 24 * 365);
  if (preferences?.timezone) response.cookies.set(preferenceCookies.timezone, preferences.timezone, preferenceCookieOptions);
  if (preferences?.theme) response.cookies.set(preferenceCookies.theme, preferences.theme, preferenceCookieOptions);
  response.cookies.set(accessCookieName, auth.accessToken, persistentCookieOptions(auth.expiresIn));
  response.cookies.set(refreshCookieName, auth.refreshToken, keepSignedIn ? persistentCookieOptions(60 * 60 * 24 * 30) : secureCookieOptions);
  response.cookies.set(activityCookieName, "active", persistentCookieOptions(auth.expiresIn));
  if (keepSignedIn) {
    response.cookies.set(persistentCookieName, "true", persistentCookieOptions(60 * 60 * 24 * 30));
  } else {
    response.cookies.delete(persistentCookieName);
  }

  return response;
}

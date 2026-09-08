import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import { deviceCookieName, newDeviceId, setDeviceCookie } from "@/features/identity/infrastructure/device-session";
import { authCookies, persistentCookieOptions, secureCookieOptions } from "@/lib/cookies";

const accessCookieName = authCookies.accessToken;
const refreshCookieName = authCookies.refreshToken;
const persistentCookieName = authCookies.persistentSession;

export async function PATCH(request: Request) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get(accessCookieName)?.value;
  const refreshToken = cookieStore.get(refreshCookieName)?.value;
  const persistentSession = cookieStore.get(persistentCookieName)?.value === "true";
  const deviceId = cookieStore.get(deviceCookieName)?.value ?? newDeviceId();
  if (!accessToken || !refreshToken) {
    return NextResponse.json(
      { error: { code: "UNAUTHORIZED", message: "Authentication is required" } },
      { status: 401 },
    );
  }

  const payload = await request.json().catch(() => null);
  if (!payload) {
    return NextResponse.json(
      { error: { code: "INVALID_REQUEST", message: "Request body is invalid" } },
      { status: 400 },
    );
  }

  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendApiUrl("/api/v1/auth/password"), {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
        "X-Device-Id": deviceId,
      },
      body: JSON.stringify({ ...payload, refreshToken }),
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
    return NextResponse.json(
      data ?? { error: { code: "PASSWORD_CHANGE_FAILED", message: "Unable to change your password" } },
      { status: backendResponse.status },
    );
  }

  const auth = data as { accessToken: string; refreshToken: string; expiresIn: number };
  const response = new NextResponse(null, { status: 204 });
  response.cookies.set(accessCookieName, auth.accessToken, persistentCookieOptions(auth.expiresIn));
  response.cookies.set(refreshCookieName, auth.refreshToken, persistentSession ? persistentCookieOptions(60 * 60 * 24 * 30) : secureCookieOptions);
  setDeviceCookie(response, deviceId);
  return response;
}

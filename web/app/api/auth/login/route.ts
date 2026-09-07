import { NextRequest, NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import { deviceCookieName, newDeviceId, setDeviceCookie } from "@/features/identity/infrastructure/device-session";

const accessCookieName = "deadlines_access_token";
const refreshCookieName = "deadlines_refresh_token";
const activityCookieName = "deadlines_last_activity";
const persistentCookieName = "deadlines_persistent_session";

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
  const response = NextResponse.json({ user: auth.user });
  const secure = process.env.NODE_ENV === "production";
  const keepSignedIn = payload.keepSignedIn === true;
  setDeviceCookie(response, deviceId);
  response.cookies.set(accessCookieName, auth.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: auth.expiresIn,
  });
  response.cookies.set(refreshCookieName, auth.refreshToken, {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    ...(keepSignedIn ? { maxAge: 60 * 60 * 24 * 30 } : {}),
  });
  response.cookies.set(activityCookieName, "active", {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    maxAge: auth.expiresIn,
  });
  if (keepSignedIn) {
    response.cookies.set(persistentCookieName, "true", { httpOnly: true, sameSite: "lax", secure, path: "/", maxAge: 60 * 60 * 24 * 30 });
  } else {
    response.cookies.delete(persistentCookieName);
  }

  return response;
}

import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

const accessCookieName = "deadlines_access_token";
const refreshCookieName = "deadlines_refresh_token";

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

export async function POST(request: Request) {
  const payload = await request.json().catch(() => null);
  if (!payload) {
    return NextResponse.json(
      { error: { code: "INVALID_REQUEST", message: "Request body is invalid" } },
      { status: 400 },
    );
  }

  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendApiUrl("/api/v1/auth/login"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
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
    maxAge: 60 * 60 * 24 * 30,
  });

  return response;
}

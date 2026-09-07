import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

const accessCookieName = "deadlines_access_token";
const sessionCookieNames = [
  accessCookieName,
  "deadlines_refresh_token",
  "deadlines_last_activity",
  "deadlines_persistent_session",
];

export async function POST(request: Request) {
  const accessToken = (await cookies()).get(accessCookieName)?.value;
  if (!accessToken) {
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
  try {
    const backendResponse = await fetch(backendApiUrl("/api/v1/users/me/deactivate"), {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify(payload),
      cache: "no-store",
    });
    const data = await backendResponse.json().catch(() => null);
    const response = data
      ? NextResponse.json(data, { status: backendResponse.status })
      : new NextResponse(null, { status: backendResponse.status });
    if (backendResponse.ok) sessionCookieNames.forEach((name) => response.cookies.delete(name));
    return response;
  } catch {
    return NextResponse.json(
      { error: { code: "BACKEND_UNAVAILABLE", message: "Account service is unavailable" } },
      { status: 503 },
    );
  }
}

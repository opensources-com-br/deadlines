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

export async function DELETE(request: Request) {
  return forwardAccountAction(request, "/api/v1/users/me", "DELETE");
}

async function forwardAccountAction(request: Request, path: string, method: string) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get(accessCookieName)?.value;
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
    const backendResponse = await fetch(backendApiUrl(path), {
      method,
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

export async function PATCH(request: Request) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get(accessCookieName)?.value;
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

  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendApiUrl("/api/v1/users/me"), {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { error: { code: "BACKEND_UNAVAILABLE", message: "Profile service is unavailable" } },
      { status: 503 },
    );
  }

  const data = await backendResponse.json().catch(() => null);
  return NextResponse.json(
    data ?? { error: { code: "PROFILE_UPDATE_FAILED", message: "Unable to update your profile" } },
    { status: backendResponse.status },
  );
}

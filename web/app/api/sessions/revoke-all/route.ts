import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

const accessCookieName = "deadlines_access_token";
const refreshCookieName = "deadlines_refresh_token";

export async function POST() {
  const accessToken = (await cookies()).get(accessCookieName)?.value;
  if (!accessToken) {
    return NextResponse.json(
      { error: { code: "UNAUTHORIZED", message: "Authentication is required" } },
      { status: 401 },
    );
  }

  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendApiUrl("/api/v1/sessions/revoke-all"), {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}` },
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { error: { code: "BACKEND_UNAVAILABLE", message: "Session service is unavailable" } },
      { status: 503 },
    );
  }

  if (!backendResponse.ok) {
    const data = await backendResponse.json().catch(() => null);
    return NextResponse.json(
      data ?? { error: { code: "SESSION_REVOKE_FAILED", message: "Unable to revoke sessions" } },
      { status: backendResponse.status },
    );
  }

  const response = new NextResponse(null, { status: 204 });
  response.cookies.delete(accessCookieName);
  response.cookies.delete(refreshCookieName);
  return response;
}

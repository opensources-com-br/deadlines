import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

const accessCookieName = "deadlines_access_token";
const refreshCookieName = "deadlines_refresh_token";

export async function POST() {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(refreshCookieName)?.value;

  if (refreshToken) {
    await fetch(backendApiUrl("/api/v1/auth/logout"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    }).catch(() => undefined);
  }

  const response = new NextResponse(null, { status: 204 });
  response.cookies.delete(accessCookieName);
  response.cookies.delete(refreshCookieName);
  return response;
}

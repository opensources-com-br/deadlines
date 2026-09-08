import { cookies } from "next/headers";
import { after, NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import { authCookies, clearLegacyCookies } from "@/lib/cookies";

const accessCookieName = authCookies.accessToken;
const refreshCookieName = authCookies.refreshToken;
const activityCookieName = authCookies.lastActivity;
const persistentCookieName = authCookies.persistentSession;

function revokeSessionAfterResponse(refreshToken: string | undefined) {
  if (!refreshToken) return;

  after(async () => {
    await fetch(backendApiUrl("/api/v1/auth/logout"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
      signal: AbortSignal.timeout(1_500),
    }).catch(() => undefined);
  });
}

function clearSessionCookies(response: NextResponse) {
  response.cookies.delete(accessCookieName);
  response.cookies.delete(refreshCookieName);
  response.cookies.delete(activityCookieName);
  response.cookies.delete(persistentCookieName);
  clearLegacyCookies(response);
  response.headers.set("Cache-Control", "private, no-store, no-cache, must-revalidate, max-age=0");
  return response;
}

async function currentRefreshToken() {
  const cookieStore = await cookies();
  return cookieStore.get(refreshCookieName)?.value;
}

export async function POST() {
  revokeSessionAfterResponse(await currentRefreshToken());
  return clearSessionCookies(new NextResponse(null, { status: 204 }));
}

export async function GET(request: Request) {
  revokeSessionAfterResponse(await currentRefreshToken());
  return clearSessionCookies(NextResponse.redirect(new URL("/login", request.url), 303));
}

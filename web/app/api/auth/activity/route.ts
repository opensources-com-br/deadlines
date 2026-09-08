import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { authCookies, persistentCookieOptions } from "@/lib/cookies";

const accessCookieName = authCookies.accessToken;
const activityCookieName = authCookies.lastActivity;
const activityWindowSeconds = 15 * 60;

export async function POST() {
  const cookieStore = await cookies();
  if (!cookieStore.get(accessCookieName)?.value) {
    return NextResponse.json({ error: { code: "UNAUTHORIZED", message: "Authentication is required" } }, { status: 401 });
  }

  const response = new NextResponse(null, { status: 204 });
  response.cookies.set(activityCookieName, "active", persistentCookieOptions(activityWindowSeconds));
  return response;
}

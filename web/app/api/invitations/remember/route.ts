import { NextResponse } from "next/server";

import { invitationCookie, persistentCookieOptions } from "@/lib/cookies";

export async function POST(request: Request) {
  const payload = (await request.json().catch(() => null)) as { token?: unknown } | null;
  if (typeof payload?.token !== "string" || !payload.token) {
    return NextResponse.json(
      { error: { code: "INVALID_REQUEST", message: "Invitation token is required" } },
      { status: 400 },
    );
  }

  const response = new NextResponse(null, { status: 204 });
  response.cookies.set(invitationCookie, payload.token, persistentCookieOptions(60 * 60 * 24 * 7));
  return response;
}

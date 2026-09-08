import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

export async function POST(request: Request) {
  const accessToken = (await cookies()).get("opensources_access_token")?.value;
  const payload = await request.json().catch(() => null);
  if (!accessToken) return NextResponse.json({ error: { code: "UNAUTHORIZED", message: "Authentication is required" } }, { status: 401 });
  if (!payload) return NextResponse.json({ error: { code: "INVALID_REQUEST", message: "Request body is invalid" } }, { status: 400 });
  try {
    const backendResponse = await fetch(backendApiUrl("/api/v1/auth/email/change"), {
      method: "POST", headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" }, body: JSON.stringify(payload), cache: "no-store",
    });
    const data = await backendResponse.json().catch(() => null);
    return data ? NextResponse.json(data, { status: backendResponse.status }) : new NextResponse(null, { status: backendResponse.status });
  } catch {
    return NextResponse.json({ error: { code: "BACKEND_UNAVAILABLE", message: "Account service is unavailable" } }, { status: 503 });
  }
}

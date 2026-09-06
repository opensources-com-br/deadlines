import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

export async function GET(request: Request) {
  const token = new URL(request.url).searchParams.get("token") ?? "";
  try {
    const response = await fetch(backendApiUrl(`/api/v1/invitations/preview?token=${encodeURIComponent(token)}`), {
      cache: "no-store",
    });
    const data = await response.json().catch(() => null);
    return NextResponse.json(
      data ?? { error: { code: "INVITATION_INVALID", message: "Invitation is invalid or has expired" } },
      { status: response.status },
    );
  } catch {
    return NextResponse.json(
      { error: { code: "BACKEND_UNAVAILABLE", message: "Invitation service is unavailable" } },
      { status: 503 },
    );
  }
}

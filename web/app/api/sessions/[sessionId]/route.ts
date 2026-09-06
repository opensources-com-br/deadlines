import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

const accessCookieName = "deadlines_access_token";

type RouteContext = {
  params: Promise<{ sessionId: string }>;
};

export async function DELETE(_request: Request, context: RouteContext) {
  const accessToken = (await cookies()).get(accessCookieName)?.value;
  if (!accessToken) {
    return NextResponse.json(
      { error: { code: "UNAUTHORIZED", message: "Authentication is required" } },
      { status: 401 },
    );
  }

  const { sessionId } = await context.params;
  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendApiUrl(`/api/v1/sessions/${encodeURIComponent(sessionId)}`), {
      method: "DELETE",
      headers: { Authorization: `Bearer ${accessToken}` },
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { error: { code: "BACKEND_UNAVAILABLE", message: "Session service is unavailable" } },
      { status: 503 },
    );
  }

  if (backendResponse.status === 204) return new NextResponse(null, { status: 204 });
  const data = await backendResponse.json().catch(() => null);
  return NextResponse.json(
    data ?? { error: { code: "SESSION_REVOKE_FAILED", message: "Unable to revoke this session" } },
    { status: backendResponse.status },
  );
}

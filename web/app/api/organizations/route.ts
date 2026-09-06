import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

const accessCookieName = "deadlines_access_token";

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

  return forwardOrganizationRequest("/api/v1/organizations", accessToken, "POST", payload);
}

async function forwardOrganizationRequest(path: string, accessToken: string, method: string, body: unknown) {
  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendApiUrl(path), {
      method,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { error: { code: "BACKEND_UNAVAILABLE", message: "Organization service is unavailable" } },
      { status: 503 },
    );
  }

  const data = await backendResponse.json().catch(() => null);
  return NextResponse.json(
    data ?? { error: { code: "ORGANIZATION_REQUEST_FAILED", message: "Unable to save your organization" } },
    { status: backendResponse.status },
  );
}

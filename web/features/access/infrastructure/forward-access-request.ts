import "server-only";

import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

type AccessMethod = "GET" | "POST" | "PATCH" | "PUT" | "DELETE";

export async function forwardAccessRequest(path: string, method: AccessMethod, request?: Request) {
  const accessToken = (await cookies()).get("deadlines_access_token")?.value;
  if (!accessToken) {
    return NextResponse.json(
      { error: { code: "UNAUTHORIZED", message: "Authentication is required" } },
      { status: 401 },
    );
  }

  let body: string | undefined;
  if (request && method !== "GET" && method !== "DELETE") {
    const payload = await request.json().catch(() => null);
    if (!payload) {
      return NextResponse.json(
        { error: { code: "INVALID_REQUEST", message: "Request body is invalid" } },
        { status: 400 },
      );
    }
    body = JSON.stringify(payload);
  }

  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendApiUrl(path), {
      method,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        ...(body ? { "Content-Type": "application/json" } : {}),
      },
      body,
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { error: { code: "BACKEND_UNAVAILABLE", message: "Access service is unavailable" } },
      { status: 503 },
    );
  }

  if (backendResponse.status === 204) return new NextResponse(null, { status: 204 });
  const data = await backendResponse.json().catch(() => null);
  return NextResponse.json(
    data ?? { error: { code: "ACCESS_REQUEST_FAILED", message: "Unable to update access settings" } },
    { status: backendResponse.status },
  );
}

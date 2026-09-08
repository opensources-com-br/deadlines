import { NextResponse, type NextRequest } from "next/server";

import { csrfCookie, csrfCookieOptions } from "@/lib/cookies";

const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

export function proxy(request: NextRequest) {
  if (request.nextUrl.pathname.startsWith("/api/") && MUTATING_METHODS.has(request.method)) {
    const origin = request.headers.get("origin");
    const csrfToken = request.cookies.get(csrfCookie)?.value;
    const csrfHeader = request.headers.get("X-CSRF-Token");
    if (origin !== request.nextUrl.origin || !csrfToken || csrfToken !== csrfHeader) {
      return NextResponse.json(
        { error: { code: "INVALID_CSRF_TOKEN", message: "Request origin or CSRF token is not allowed" } },
        { status: 403 },
      );
    }
  }

  const response = NextResponse.next();
  if (!request.cookies.get(csrfCookie)?.value) {
    response.cookies.set(csrfCookie, crypto.randomUUID(), csrfCookieOptions);
  }
  return response;
}

export const config = { matcher: "/((?!_next/static|_next/image|favicon.ico).*)" };

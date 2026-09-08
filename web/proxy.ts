import { NextResponse, type NextRequest } from "next/server";

const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

export function proxy(request: NextRequest) {
  if (!MUTATING_METHODS.has(request.method)) return NextResponse.next();

  const origin = request.headers.get("origin");
  if (origin === request.nextUrl.origin) return NextResponse.next();

  return NextResponse.json(
    { error: { code: "INVALID_ORIGIN", message: "Request origin is not allowed" } },
    { status: 403 },
  );
}

export const config = { matcher: "/api/:path*" };

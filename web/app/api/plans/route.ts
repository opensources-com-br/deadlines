import { NextResponse } from "next/server";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

export async function GET() {
  const response = await fetch(backendApiUrl("/api/v1/plans"), { cache: "no-store" }).catch(() => undefined);
  if (!response) return NextResponse.json({ error: { message: "Plan catalog is unavailable" } }, { status: 503 });
  const data = await response.json().catch(() => null);
  return NextResponse.json(data ?? { error: { message: "Plan catalog is unavailable" } }, { status: response.status });
}

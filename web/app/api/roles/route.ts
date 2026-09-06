import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

export async function GET() {
  return forwardAccessRequest("/api/v1/roles", "GET");
}

export async function POST(request: Request) {
  return forwardAccessRequest("/api/v1/roles", "POST", request);
}

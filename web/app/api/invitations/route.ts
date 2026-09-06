import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

export async function GET() {
  return forwardAccessRequest("/api/v1/invitations", "GET");
}

export async function POST(request: Request) {
  return forwardAccessRequest("/api/v1/invitations", "POST", request);
}

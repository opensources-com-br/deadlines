import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

export async function POST(request: Request) {
  return forwardAccessRequest("/api/v1/invitations/accept", "POST", request);
}

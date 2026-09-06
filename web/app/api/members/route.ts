import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

export async function GET() {
  return forwardAccessRequest("/api/v1/members", "GET");
}

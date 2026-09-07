import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

export function GET() {
  return forwardAccessRequest("/api/v1/users/me/authorization", "GET");
}

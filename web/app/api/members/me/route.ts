import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

export async function DELETE() {
  return forwardAccessRequest("/api/v1/members/me", "DELETE");
}

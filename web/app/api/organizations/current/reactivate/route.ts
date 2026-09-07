import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

export async function POST() {
  return forwardAccessRequest("/api/v1/organizations/current/reactivate", "POST");
}

import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

export async function POST(request: Request) {
  const response = await forwardAccessRequest("/api/v1/invitations/accept", "POST", request);
  if (response.ok) response.cookies.delete("deadlines_invitation_token");
  return response;
}

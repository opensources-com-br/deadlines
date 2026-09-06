import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

type RouteContext = { params: Promise<{ invitationId: string }> };

export async function POST(_request: Request, context: RouteContext) {
  const { invitationId } = await context.params;
  return forwardAccessRequest(`/api/v1/invitations/${encodeURIComponent(invitationId)}/resend`, "POST");
}

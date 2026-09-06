import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

type RouteContext = { params: Promise<{ invitationId: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { invitationId } = await context.params;
  return forwardAccessRequest(`/api/v1/invitations/${encodeURIComponent(invitationId)}`, "GET");
}

export async function DELETE(_request: Request, context: RouteContext) {
  const { invitationId } = await context.params;
  return forwardAccessRequest(`/api/v1/invitations/${encodeURIComponent(invitationId)}`, "DELETE");
}

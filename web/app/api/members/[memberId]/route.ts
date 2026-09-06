import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

type RouteContext = { params: Promise<{ memberId: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { memberId } = await context.params;
  return forwardAccessRequest(`/api/v1/members/${encodeURIComponent(memberId)}`, "GET");
}

export async function PATCH(request: Request, context: RouteContext) {
  const { memberId } = await context.params;
  return forwardAccessRequest(`/api/v1/members/${encodeURIComponent(memberId)}`, "PATCH", request);
}

export async function DELETE(_request: Request, context: RouteContext) {
  const { memberId } = await context.params;
  return forwardAccessRequest(`/api/v1/members/${encodeURIComponent(memberId)}`, "DELETE");
}

import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

type RouteContext = { params: Promise<{ memberId: string }> };

export async function POST(_request: Request, context: RouteContext) {
  const { memberId } = await context.params;
  return forwardAccessRequest(`/api/v1/members/${encodeURIComponent(memberId)}/suspend`, "POST");
}

import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

type RouteContext = { params: Promise<{ roleId: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { roleId } = await context.params;
  return forwardAccessRequest(`/api/v1/roles/${encodeURIComponent(roleId)}/permissions`, "GET");
}

export async function PUT(request: Request, context: RouteContext) {
  const { roleId } = await context.params;
  return forwardAccessRequest(`/api/v1/roles/${encodeURIComponent(roleId)}/permissions`, "PUT", request);
}

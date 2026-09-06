import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

type RouteContext = { params: Promise<{ roleId: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { roleId } = await context.params;
  return forwardAccessRequest(`/api/v1/roles/${encodeURIComponent(roleId)}`, "GET");
}

export async function PATCH(request: Request, context: RouteContext) {
  const { roleId } = await context.params;
  return forwardAccessRequest(`/api/v1/roles/${encodeURIComponent(roleId)}`, "PATCH", request);
}

export async function DELETE(_request: Request, context: RouteContext) {
  const { roleId } = await context.params;
  return forwardAccessRequest(`/api/v1/roles/${encodeURIComponent(roleId)}`, "DELETE");
}

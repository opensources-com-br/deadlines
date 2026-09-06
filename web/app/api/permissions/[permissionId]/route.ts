import { forwardAccessRequest } from "@/features/access/infrastructure/forward-access-request";

type RouteContext = { params: Promise<{ permissionId: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { permissionId } = await context.params;
  return forwardAccessRequest(`/api/v1/permissions/${encodeURIComponent(permissionId)}`, "GET");
}

export async function PATCH(request: Request, context: RouteContext) {
  const { permissionId } = await context.params;
  return forwardAccessRequest(`/api/v1/permissions/${encodeURIComponent(permissionId)}`, "PATCH", request);
}

export async function DELETE(_request: Request, context: RouteContext) {
  const { permissionId } = await context.params;
  return forwardAccessRequest(`/api/v1/permissions/${encodeURIComponent(permissionId)}`, "DELETE");
}

import type { AccessInput, AccessList, Permission, Role } from "@/features/access/domain/access";

type ErrorPayload = { error?: { message?: string } };

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(path, options);
  if (response.status === 204) return undefined as T;
  const data = (await response.json().catch(() => ({}))) as T & ErrorPayload;
  if (!response.ok) throw new Error(data.error?.message ?? "Unable to update access settings.");
  return data;
}

const json = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

export const accessApi = {
  listPermissions: () => request<AccessList<Permission>>("/api/permissions"),
  createPermission: (input: AccessInput) => request<Permission>("/api/permissions", json("POST", input)),
  updatePermission: (id: string, input: Partial<AccessInput>) =>
    request<Permission>(`/api/permissions/${id}`, json("PATCH", input)),
  deletePermission: (id: string) => request<void>(`/api/permissions/${id}`, { method: "DELETE" }),
  listRoles: () => request<AccessList<Role>>("/api/roles"),
  createRole: (input: AccessInput) => request<Role>("/api/roles", json("POST", input)),
  updateRole: (id: string, input: Partial<AccessInput>) =>
    request<Role>(`/api/roles/${id}`, json("PATCH", input)),
  deleteRole: (id: string) => request<void>(`/api/roles/${id}`, { method: "DELETE" }),
  listRolePermissions: (roleId: string) =>
    request<AccessList<Permission>>(`/api/roles/${roleId}/permissions`),
  replaceRolePermissions: (roleId: string, permissionIds: string[]) =>
    request<AccessList<Permission>>(
      `/api/roles/${roleId}/permissions`,
      json("PUT", { permissionIds }),
    ),
};

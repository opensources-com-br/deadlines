import type { AccessInput, AccessList, Permission, Role } from "@/features/access/domain/access";
import { apiClient } from "@/lib/api-client";

export const accessApi = {
  listPermissions: () => apiClient.get<AccessList<Permission>>("/api/permissions"),
  createPermission: (input: AccessInput) => apiClient.post<Permission>("/api/permissions", input),
  updatePermission: (id: string, input: Partial<AccessInput>) =>
    apiClient.patch<Permission>(`/api/permissions/${id}`, input),
  deletePermission: (id: string) => apiClient.delete<void>(`/api/permissions/${id}`),
  listRoles: () => apiClient.get<AccessList<Role>>("/api/roles"),
  createRole: (input: AccessInput) => apiClient.post<Role>("/api/roles", input),
  updateRole: (id: string, input: Partial<AccessInput>) =>
    apiClient.patch<Role>(`/api/roles/${id}`, input),
  deleteRole: (id: string) => apiClient.delete<void>(`/api/roles/${id}`),
  listRolePermissions: (roleId: string) =>
    apiClient.get<AccessList<Permission>>(`/api/roles/${roleId}/permissions`),
  replaceRolePermissions: (roleId: string, permissionIds: string[]) =>
    apiClient.put<AccessList<Permission>>(
      `/api/roles/${roleId}/permissions`,
      { permissionIds },
    ),
};

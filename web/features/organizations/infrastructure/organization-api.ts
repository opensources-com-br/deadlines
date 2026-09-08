import type {
  CreateOrganizationInput,
  Organization,
  UpdateOrganizationInput,
} from "@/features/organizations/domain/organization";
import { apiClient } from "@/lib/api-client";

export const organizationApi = {
  create: (input: CreateOrganizationInput) => apiClient.post<Organization>("/api/organizations", input),
  update: (input: UpdateOrganizationInput) => apiClient.patch<Organization>("/api/organizations/current", input),
  suspend: () => apiClient.post<Organization>("/api/organizations/current/suspend"),
  reactivate: () => apiClient.post<Organization>("/api/organizations/current/reactivate"),
  delete: () => apiClient.delete<void>("/api/organizations/current"),
};

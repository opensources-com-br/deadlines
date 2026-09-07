import type {
  CreateOrganizationInput,
  Organization,
  UpdateOrganizationInput,
} from "@/features/organizations/domain/organization";

type ErrorPayload = {
  error?: {
    message?: string;
  };
};

async function request(path: string, method: "POST" | "PATCH", body: unknown): Promise<Organization> {
  const response = await fetch(path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = (await response.json().catch(() => ({}))) as Organization & ErrorPayload;
  if (!response.ok) {
    throw new Error(data.error?.message ?? "Unable to save your organization.");
  }
  return data;
}

async function lifecycleRequest(path: string, method: "POST" | "DELETE"): Promise<Organization | undefined> {
  const response = await fetch(path, { method });
  if (response.status === 204) return undefined;
  const data = (await response.json().catch(() => ({}))) as Organization & ErrorPayload;
  if (!response.ok) throw new Error(data.error?.message ?? "Unable to update your organization.");
  return data;
}

export const organizationApi = {
  create: (input: CreateOrganizationInput) => request("/api/organizations", "POST", input),
  update: (input: UpdateOrganizationInput) => request("/api/organizations/current", "PATCH", input),
  suspend: () => lifecycleRequest("/api/organizations/current/suspend", "POST") as Promise<Organization>,
  reactivate: () => lifecycleRequest("/api/organizations/current/reactivate", "POST") as Promise<Organization>,
  delete: () => lifecycleRequest("/api/organizations/current", "DELETE"),
};

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

export const organizationApi = {
  create: (input: CreateOrganizationInput) => request("/api/organizations", "POST", input),
  update: (input: UpdateOrganizationInput) => request("/api/organizations/current", "PATCH", input),
};

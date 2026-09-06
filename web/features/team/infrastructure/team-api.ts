import type {
  InvitationPreview,
  OrganizationInvitation,
  OrganizationMember,
  TeamList,
} from "@/features/team/domain/team";

type ErrorPayload = { error?: { message?: string } };

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(path, options);
  if (response.status === 204) return undefined as T;
  const data = (await response.json().catch(() => ({}))) as T & ErrorPayload;
  if (!response.ok) throw new Error(data.error?.message ?? "Unable to update your team.");
  return data;
}

const json = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

export const teamApi = {
  listMembers: () => request<TeamList<OrganizationMember>>("/api/members"),
  updateMemberRole: (memberId: string, roleId: string) =>
    request<OrganizationMember>(`/api/members/${memberId}`, json("PATCH", { roleId })),
  removeMember: (memberId: string) => request<void>(`/api/members/${memberId}`, { method: "DELETE" }),
  listInvitations: () => request<TeamList<OrganizationInvitation>>("/api/invitations"),
  createInvitation: (email: string, roleId: string) =>
    request<OrganizationInvitation>("/api/invitations", json("POST", { email, roleId })),
  resendInvitation: (invitationId: string) =>
    request<OrganizationInvitation>(`/api/invitations/${invitationId}/resend`, { method: "POST" }),
  revokeInvitation: (invitationId: string) =>
    request<void>(`/api/invitations/${invitationId}`, { method: "DELETE" }),
  previewInvitation: (token: string) =>
    request<InvitationPreview>(`/api/invitations/preview?token=${encodeURIComponent(token)}`),
  acceptInvitation: (token: string) =>
    request<OrganizationMember>("/api/invitations/accept", json("POST", { token })),
};

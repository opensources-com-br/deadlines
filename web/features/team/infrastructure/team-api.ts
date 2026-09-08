import type {
  InvitationPreview,
  OrganizationInvitation,
  OrganizationMember,
  TeamList,
} from "@/features/team/domain/team";
import { apiClient } from "@/lib/api-client";

export const teamApi = {
  listMembers: () => apiClient.get<TeamList<OrganizationMember>>("/api/members"),
  updateMemberRole: (memberId: string, roleId: string) =>
    apiClient.patch<OrganizationMember>(`/api/members/${memberId}`, { roleId }),
  removeMember: (memberId: string) => apiClient.delete<void>(`/api/members/${memberId}`),
  suspendMember: (memberId: string) =>
    apiClient.post<OrganizationMember>(`/api/members/${memberId}/suspend`),
  reactivateMember: (memberId: string) =>
    apiClient.post<OrganizationMember>(`/api/members/${memberId}/reactivate`),
  leaveOrganization: () => apiClient.delete<void>("/api/members/me"),
  transferOwnership: (memberId: string, previousOwnerRoleId: string) =>
    apiClient.post<OrganizationMember>(
      `/api/members/${memberId}/transfer-ownership`,
      { previousOwnerRoleId },
    ),
  listInvitations: () => apiClient.get<TeamList<OrganizationInvitation>>("/api/invitations"),
  createInvitation: (email: string, roleId: string) =>
    apiClient.post<OrganizationInvitation>("/api/invitations", { email, roleId }),
  resendInvitation: (invitationId: string) =>
    apiClient.post<OrganizationInvitation>(`/api/invitations/${invitationId}/resend`),
  revokeInvitation: (invitationId: string) => apiClient.delete<void>(`/api/invitations/${invitationId}`),
  previewInvitation: (token: string) =>
    apiClient.get<InvitationPreview>(`/api/invitations/preview?token=${encodeURIComponent(token)}`),
  acceptInvitation: (token: string) =>
    apiClient.post<OrganizationMember>("/api/invitations/accept", { token }),
};

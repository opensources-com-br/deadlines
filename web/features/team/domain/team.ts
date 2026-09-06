import type { Role } from "@/features/access/domain/access";

export type OrganizationMember = {
  id: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  joinedAt: string;
};

export type OrganizationInvitation = {
  id: string;
  organizationId: string;
  organizationName: string;
  email: string;
  role: Role;
  status: "pending" | "accepted" | "revoked" | "expired";
  expiresAt: string;
  createdAt: string;
  updatedAt: string;
};

export type InvitationPreview = {
  organizationName: string;
  email: string;
  roleName: string;
  status: "pending" | "accepted" | "revoked" | "expired";
  expiresAt: string;
};

export type TeamList<T> = { data: T[] };

import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import type { Organization } from "@/features/organizations/domain/organization";
import type { AccessList, Permission, Role } from "@/features/access/domain/access";
import type { SessionList } from "@/features/platform/domain/session";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { PlatformHome } from "@/features/platform/presentation/PlatformHome";
import type { OrganizationInvitation, OrganizationMember, TeamList } from "@/features/team/domain/team";

export default async function PlatformPage() {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("deadlines_access_token")?.value;
  if (!accessToken) {
    redirect("/login");
  }

  const authenticatedRequest = {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store" as const,
  };
  const [response, sessionsResponse, organizationResponse, permissionsResponse, rolesResponse, membersResponse, invitationsResponse] = await Promise.all([
    fetch(backendApiUrl("/api/v1/users/me"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/sessions"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/organizations/current"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/permissions"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/roles"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/members"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/invitations"), authenticatedRequest).catch(() => undefined),
  ]);

  if (!response?.ok) {
    redirect("/login");
  }
  if (organizationResponse?.status === 404) {
    redirect("/onboarding/organization");
  }
  if (!organizationResponse?.ok) {
    redirect("/login");
  }

  const user = (await response.json()) as UserProfile;
  const organization = (await organizationResponse.json()) as Organization;
  const sessions = sessionsResponse?.ok
    ? ((await sessionsResponse.json()) as SessionList).data
    : [];
  const permissions = permissionsResponse?.ok
    ? ((await permissionsResponse.json()) as AccessList<Permission>).data
    : [];
  const roles = rolesResponse?.ok ? ((await rolesResponse.json()) as AccessList<Role>).data : [];
  const members = membersResponse?.ok ? ((await membersResponse.json()) as TeamList<OrganizationMember>).data : [];
  const invitations = invitationsResponse?.ok
    ? ((await invitationsResponse.json()) as TeamList<OrganizationInvitation>).data
    : [];
  return (
    <PlatformHome
      user={user}
      organization={organization}
      sessions={sessions}
      permissions={permissions}
      roles={roles}
      members={members}
      invitations={invitations}
    />
  );
}

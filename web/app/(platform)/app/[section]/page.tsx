import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";

import type { AccessList, Permission, Role } from "@/features/access/domain/access";
import type { AuthorizationContext } from "@/features/access/domain/authorization";
import { platformPermission } from "@/features/access/domain/authorization";
import { AuthorizationProvider } from "@/features/access/presentation/AuthorizationProvider";
import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import type { Organization } from "@/features/organizations/domain/organization";
import type { SessionList } from "@/features/platform/domain/session";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { PlatformHome } from "@/features/platform/presentation/PlatformHome";
import type { PlatformNavigationItem, SettingsSection } from "@/features/platform/presentation/PlatformSidebar";
import type { OrganizationInvitation, OrganizationMember, TeamList } from "@/features/team/domain/team";

const sections = new Set<PlatformNavigationItem>([
  "organization",
  "plans",
  "team",
  "access-control",
  "security",
  "account",
  "notifications",
  "settings",
]);

const settingsSections = new Set<SettingsSection>([
  "organization",
  "plans",
  "team",
  "access-control",
  "security",
  "account",
  "notifications",
]);

type PlatformSectionPageProps = {
  params: Promise<{ section: string }>;
  searchParams: Promise<{ section?: string }>;
};

export default async function PlatformSectionPage({ params, searchParams }: PlatformSectionPageProps) {
  const { section } = await params;
  const { section: settingsSection } = await searchParams;
  if (!sections.has(section as PlatformNavigationItem)) notFound();

  const cookieStore = await cookies();
  const accessToken = cookieStore.get("deadlines_access_token")?.value;
  const refreshToken = cookieStore.get("deadlines_refresh_token")?.value;
  const recentActivity = cookieStore.get("deadlines_last_activity")?.value;
  const persistentSession = cookieStore.get("deadlines_persistent_session")?.value === "true";
  const returnTo = `/app/${section}`;
  if (!accessToken) redirect(refreshToken && (recentActivity || persistentSession) ? `/api/auth/refresh?returnTo=${encodeURIComponent(returnTo)}` : "/login");

  const authenticatedRequest = {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store" as const,
  };
  const [response, sessionsResponse, authorizationResponse] = await Promise.all([
    fetch(backendApiUrl("/api/v1/users/me"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/sessions"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/users/me/authorization"), authenticatedRequest).catch(() => undefined),
  ]);

  if (!response?.ok) redirect(refreshToken && (recentActivity || persistentSession) && response?.status === 401 ? `/api/auth/refresh?returnTo=${encodeURIComponent(returnTo)}` : "/login");
  if (authorizationResponse?.status === 404) redirect("/onboarding/organization");
  if (!authorizationResponse?.ok) redirect("/login");

  const user = (await response.json()) as UserProfile;
  const authorization = (await authorizationResponse.json()) as AuthorizationContext;
  const can = (permission: string) => authorization.permissions.includes(permission);
  const fetchWhen = (allowed: boolean, path: string) => allowed
    ? fetch(backendApiUrl(path), authenticatedRequest).catch(() => undefined)
    : Promise.resolve(undefined);
  const [organizationResponse, permissionsResponse, rolesResponse, membersResponse, invitationsResponse] = await Promise.all([
    fetchWhen(can(platformPermission.organizationRead), "/api/v1/organizations/current"),
    fetchWhen(can(platformPermission.permissionsRead), "/api/v1/permissions"),
    fetchWhen(can(platformPermission.rolesRead), "/api/v1/roles"),
    fetchWhen(can(platformPermission.membersRead), "/api/v1/members"),
    fetchWhen(can(platformPermission.membersInvite), "/api/v1/invitations"),
  ]);

  const organization = organizationResponse?.ok ? (await organizationResponse.json()) as Organization : null;
  const sessions = sessionsResponse?.ok ? ((await sessionsResponse.json()) as SessionList).data : [];
  const permissions = permissionsResponse?.ok ? ((await permissionsResponse.json()) as AccessList<Permission>).data : [];
  const roles = rolesResponse?.ok ? ((await rolesResponse.json()) as AccessList<Role>).data : [];
  const members = membersResponse?.ok ? ((await membersResponse.json()) as TeamList<OrganizationMember>).data : [];
  const invitations = invitationsResponse?.ok ? ((await invitationsResponse.json()) as TeamList<OrganizationInvitation>).data : [];

  return (
    <AuthorizationProvider authorization={authorization}>
      <PlatformHome
        user={user}
        organization={organization}
        sessions={sessions}
        permissions={permissions}
        roles={roles}
        members={members}
        invitations={invitations}
        section={section as PlatformNavigationItem}
        settingsSection={settingsSections.has(settingsSection as SettingsSection) ? settingsSection as SettingsSection : undefined}
      />
    </AuthorizationProvider>
  );
}

import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import type { Organization } from "@/features/organizations/domain/organization";
import type { SessionList } from "@/features/platform/domain/session";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { PlatformHome } from "@/features/platform/presentation/PlatformHome";

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
  const [response, sessionsResponse, organizationResponse] = await Promise.all([
    fetch(backendApiUrl("/api/v1/users/me"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/sessions"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/organizations/current"), authenticatedRequest).catch(() => undefined),
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
  return <PlatformHome user={user} organization={organization} sessions={sessions} />;
}

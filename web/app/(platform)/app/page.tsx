import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { PlatformBlankHome } from "@/features/platform/presentation/PlatformBlankHome";

export default async function PlatformPage() {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("deadlines_access_token")?.value;
  const refreshToken = cookieStore.get("deadlines_refresh_token")?.value;
  const recentActivity = cookieStore.get("deadlines_last_activity")?.value;
  const persistentSession = cookieStore.get("deadlines_persistent_session")?.value === "true";
  if (!accessToken) redirect(refreshToken && (recentActivity || persistentSession) ? "/api/auth/refresh?returnTo=/app" : "/login");

  const authenticatedRequest = {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store" as const,
  };
  const [userResponse, organizationResponse] = await Promise.all([
    fetch(backendApiUrl("/api/v1/users/me"), authenticatedRequest).catch(() => undefined),
    fetch(backendApiUrl("/api/v1/organizations/current"), authenticatedRequest).catch(() => undefined),
  ]);

  if (!userResponse?.ok) redirect(refreshToken && (recentActivity || persistentSession) && userResponse?.status === 401 ? "/api/auth/refresh?returnTo=/app" : "/login");
  if (organizationResponse?.status === 404) redirect("/onboarding/organization");
  if (!organizationResponse?.ok) redirect("/login");

  return <PlatformBlankHome user={(await userResponse.json()) as UserProfile} />;
}

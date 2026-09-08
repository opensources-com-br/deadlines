import type { ReactNode } from "react";
import { cookies } from "next/headers";

import { PlatformSessionGuard } from "@/features/platform/presentation/PlatformSessionGuard";
import { UserPreferenceProvider } from "@/features/platform/presentation/UserPreferenceProvider";
import type { UserPreference } from "@/features/platform/domain/user-preference";
import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";

type PlatformLayoutProps = {
  children: ReactNode;
};

export default async function PlatformLayout({ children }: PlatformLayoutProps) {
  const cookieStore = await cookies();
  const persistentSession = cookieStore.get("opensources_persistent_session")?.value === "true";
  const accessToken = cookieStore.get("opensources_access_token")?.value;
  const fallback: UserPreference = { locale: "pt-BR", timezone: "America/Sao_Paulo", theme: "system", updatedAt: new Date(0).toISOString() };
  const preferences = accessToken ? await fetch(backendApiUrl("/api/v1/users/me/preferences"), {
    headers: { Authorization: `Bearer ${accessToken}` }, cache: "no-store",
  }).then((response) => response.ok ? response.json() as Promise<UserPreference> : fallback).catch(() => fallback) : fallback;
  return (
    <UserPreferenceProvider initialPreferences={preferences}>
      <PlatformSessionGuard persistentSession={persistentSession} />
      {children}
    </UserPreferenceProvider>
  );
}

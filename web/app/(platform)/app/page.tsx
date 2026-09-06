import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import { PlatformHome } from "@/features/platform/presentation/PlatformHome";

type CurrentUser = {
  email: string;
  profile: {
    firstName: string;
    lastName: string;
  };
};

export default async function PlatformPage() {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("deadlines_access_token")?.value;
  if (!accessToken) {
    redirect("/login");
  }

  const response = await fetch(backendApiUrl("/api/v1/auth/me"), {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store",
  }).catch(() => undefined);

  if (!response?.ok) {
    redirect("/login");
  }

  const user = (await response.json()) as CurrentUser;
  return <PlatformHome user={{ email: user.email, ...user.profile }} />;
}

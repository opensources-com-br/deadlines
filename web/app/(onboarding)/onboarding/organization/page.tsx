import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { backendApiUrl } from "@/features/identity/infrastructure/backend-api";
import { CreateOrganizationScreen } from "@/features/organizations/presentation/CreateOrganizationScreen";

export default async function CreateOrganizationPage() {
  const accessToken = (await cookies()).get("deadlines_access_token")?.value;
  if (!accessToken) redirect("/login");

  const response = await fetch(backendApiUrl("/api/v1/organizations/current"), {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store",
  }).catch(() => undefined);

  if (response?.ok) redirect("/app");
  if (response && response.status !== 404) redirect("/login");

  return <CreateOrganizationScreen />;
}

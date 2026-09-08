import { notFound, redirect } from "next/navigation";

import type { SettingsSection } from "@/features/platform/presentation/PlatformSidebar";
import { billingEnabled } from "@/lib/features";

const legacySettingsSections = new Set<SettingsSection>([
  "organization",
  "plans",
  "team",
  "access-control",
  "security",
  "account",
  "notifications",
]);

type LegacyPlatformSectionPageProps = {
  params: Promise<{ section: string }>;
};

export default async function LegacyPlatformSectionPage({ params }: LegacyPlatformSectionPageProps) {
  const { section } = await params;
  if (!legacySettingsSections.has(section as SettingsSection)) notFound();
  if (!billingEnabled && section === "plans") notFound();

  redirect(`/app/settings/${section}`);
}

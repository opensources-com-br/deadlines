"use client";

import { AuditsCard } from "@/features/audits/presentation/AuditsCard";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { UserSession } from "@/features/platform/domain/session";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { AccountSettings } from "@/features/platform/presentation/AccountSettings";
import { NotificationsCard } from "@/features/platform/presentation/NotificationsCard";
import { SessionsCard } from "@/features/platform/presentation/SessionsCard";
import { PlatformSidebar, type SettingsSection } from "@/features/platform/presentation/PlatformSidebar";
import type { Organization } from "@/features/organizations/domain/organization";
import { OrganizationCard } from "@/features/organizations/presentation/OrganizationCard";
import type { Permission, Role } from "@/features/access/domain/access";
import { PermissionsCard } from "@/features/access/presentation/PermissionsCard";
import { RolesCard } from "@/features/access/presentation/RolesCard";
import { platformPermission } from "@/features/access/domain/authorization";
import { Can } from "@/features/access/presentation/Can";
import type { OrganizationInvitation, OrganizationMember } from "@/features/team/domain/team";
import { InvitationsCard } from "@/features/team/presentation/InvitationsCard";
import { MembersCard } from "@/features/team/presentation/MembersCard";
import { PlansCard } from "@/features/plans/presentation/PlansCard";

type PlatformHomeProps = {
  user: UserProfile;
  organization: Organization | null;
  sessions: UserSession[];
  permissions: Permission[];
  roles: Role[];
  members: OrganizationMember[];
  invitations: OrganizationInvitation[];
  section: SettingsSection;
};

export function PlatformHome({ user, organization, sessions, permissions, roles, members, invitations, section }: PlatformHomeProps) {
  const t = useTranslations("SettingsShell");
  const tPublic = useTranslations("Public");
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [availablePermissions, setAvailablePermissions] = useState(permissions);
  const sectionDetails: Record<SettingsSection, { eyebrow: string; title: string; description: string }> = {
    organization: { eyebrow: t("workspace"), title: t("organizationTitle"), description: t("organizationDescription") },
    plans: { eyebrow: t("workspace"), title: t("plansTitle"), description: t("plansDescription") },
    team: { eyebrow: t("management"), title: t("teamTitle"), description: t("teamDescription") },
    "access-control": { eyebrow: t("management"), title: t("accessTitle"), description: t("accessDescription") },
    security: { eyebrow: t("security"), title: t("security"), description: t("securityDescription") },
    account: { eyebrow: t("account"), title: t("accountTitle"), description: t("accountDescription") },
    notifications: { eyebrow: t("account"), title: t("notifications"), description: t("notificationsDescription") },
  };
  const workspaceItems: Array<{ key: SettingsSection; label: string }> = [
    { key: "organization", label: t("general") },
    { key: "plans", label: t("plans") },
    { key: "team", label: t("users") },
  ];
  const accessItems: Array<{ key: SettingsSection; label: string }> = [
    { key: "access-control", label: t("roles") },
  ];
  const settingsNavigation: Array<{ key: string; label: string; items: Array<{ key: SettingsSection; label: string }> }> = [
    { key: "organization", label: t("organization"), items: workspaceItems },
    { key: "access", label: t("access"), items: accessItems },
    { key: "personal", label: t("personal"), items: [{ key: "account", label: t("account") }, { key: "notifications", label: t("notifications") }, { key: "security", label: t("security") }] },
  ];
  const visibleSettingsSections = settingsNavigation.flatMap((group) => group.items.map((item) => item.key));
  const activeSettingsSection = visibleSettingsSections.includes(section)
    ? section
    : (visibleSettingsSections[0] ?? "account");
  const details = sectionDetails[activeSettingsSection];
  const activeGroup = settingsNavigation.find((group) => group.items.some((item) => item.key === activeSettingsSection)) ?? settingsNavigation[0];

  function handleSignOut() {
    setIsSigningOut(true);
    window.location.replace("/api/auth/logout");
  }

  return (
    <SidebarProvider>
      <PlatformSidebar
        activeItem="settings"
        user={user}
        onSignOut={handleSignOut}
        isSigningOut={isSigningOut}
      />
      <SidebarInset className="min-h-svh bg-background text-foreground">
      <header className="w-full border-b border-border py-4">
        <div className="flex w-full items-center px-[30px]">
          <SidebarTrigger variant="ghost" aria-label={tPublic("toggleSidebar")} />
        </div>
      </header>

      <section className="w-full px-[30px] pb-[34px] pt-[22px]">
        <h1 className="sr-only">{t("settings")}</h1>
        <Tabs value={activeGroup.key}>
          <TabsList aria-label={t("settings")}>
            {settingsNavigation.map((group) => (
              <TabsTrigger
                key={group.key}
                value={group.key}
                nativeButton={false}
                render={<Link href={`/app/settings/${group.items[0].key}`} />}
              >
                {group.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
        <div className="mt-6 rounded-2xl border bg-card/40 p-4 sm:p-6">
          <div className="flex flex-col gap-5 border-b pb-5">
            <div>
              <p className="text-sm font-medium text-muted-foreground">{t("categoryBreadcrumb", { category: activeGroup.label })}</p>
              <h2 className="mt-1 text-lg font-semibold tracking-tight">{t("categoryTitle", { category: activeGroup.label })}</h2>
            </div>
            <Tabs value={activeSettingsSection}>
              <TabsList aria-label={`${activeGroup.label} settings`}>
                {activeGroup.items.map((item) => (
                  <TabsTrigger
                    key={item.key}
                    value={item.key}
                    nativeButton={false}
                    render={<Link href={`/app/settings/${item.key}`} />}
                  >
                    {item.label}
                  </TabsTrigger>
                ))}
              </TabsList>
            </Tabs>
          </div>
        <div className="space-y-6 pt-6">
        <div>
          <p className="text-sm font-medium text-muted-foreground">{details.eyebrow}</p>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight">{details.title}</h3>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">{details.description}</p>
        </div>
        {activeSettingsSection === "organization" && organization ? <Can permission={platformPermission.organizationRead}><OrganizationCard organization={organization} /></Can> : null}
        {activeSettingsSection === "plans" && <Can permission={platformPermission.billingRead}><PlansCard /></Can>}
        {activeSettingsSection === "team" && <Can permission={platformPermission.membersRead}><MembersCard initialMembers={members} roles={roles} /></Can>}
        {activeSettingsSection === "team" && <Can permission={platformPermission.membersInvite}><InvitationsCard initialInvitations={invitations} roles={roles} /></Can>}
        {activeSettingsSection === "access-control" && <Can permission={platformPermission.permissionsRead}><PermissionsCard
          initialPermissions={availablePermissions}
          onPermissionsChange={setAvailablePermissions}
        /></Can>}
        {activeSettingsSection === "access-control" && <Can permission={platformPermission.rolesRead}><RolesCard initialRoles={roles} permissions={availablePermissions} /></Can>}
        {activeSettingsSection === "account" && <AccountSettings user={user} />}
        {activeSettingsSection === "notifications" && <NotificationsCard />}
        {activeSettingsSection === "security" && <Can permission={platformPermission.auditRead}><AuditsCard members={members} /></Can>}
        {activeSettingsSection === "security" && <SessionsCard initialSessions={sessions} />}
        </div>
        </div>
      </section>
      </SidebarInset>
    </SidebarProvider>
  );
}

"use client";

import { AuditsCard } from "@/features/audits/presentation/AuditsCard";

import Link from "next/link";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import type { UserSession } from "@/features/platform/domain/session";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { AccountSettings } from "@/features/platform/presentation/AccountSettings";
import { NotificationsCard } from "@/features/platform/presentation/NotificationsCard";
import { SessionsCard } from "@/features/platform/presentation/SessionsCard";
import { PlatformSidebar, type PlatformNavigationItem, type SettingsSection } from "@/features/platform/presentation/PlatformSidebar";
import type { Organization } from "@/features/organizations/domain/organization";
import { OrganizationCard } from "@/features/organizations/presentation/OrganizationCard";
import type { Permission, Role } from "@/features/access/domain/access";
import { PermissionsCard } from "@/features/access/presentation/PermissionsCard";
import { RolesCard } from "@/features/access/presentation/RolesCard";
import type { OrganizationInvitation, OrganizationMember } from "@/features/team/domain/team";
import { InvitationsCard } from "@/features/team/presentation/InvitationsCard";
import { MembersCard } from "@/features/team/presentation/MembersCard";
import { PlansCard } from "@/features/plans/presentation/PlansCard";

type PlatformHomeProps = {
  user: UserProfile;
  organization: Organization;
  sessions: UserSession[];
  permissions: Permission[];
  roles: Role[];
  members: OrganizationMember[];
  invitations: OrganizationInvitation[];
  section: PlatformNavigationItem;
  settingsSection?: SettingsSection;
};

const sectionDetails: Record<SettingsSection, { eyebrow: string; title: string; description: string }> = {
  organization: { eyebrow: "Workspace", title: "Organization", description: "Manage your organization and its workspace details." },
  plans: { eyebrow: "Workspace", title: "Plans", description: "Review your organization’s current subscription." },
  team: { eyebrow: "Management", title: "Team", description: "Manage members and invitations for your organization." },
  "access-control": { eyebrow: "Management", title: "Access control", description: "Manage roles and permissions for your organization." },
  security: { eyebrow: "Security", title: "Security", description: "Review organization history and active sessions." },
  account: { eyebrow: "Account", title: "Your account", description: "Manage your personal information and password." },
  notifications: { eyebrow: "Account", title: "Notifications", description: "Manage how you receive account updates." },
};

const settingsNavigation: Array<{ label: string; items: Array<{ key: SettingsSection; label: string }> }> = [
  { label: "Organization", items: [{ key: "organization", label: "General" }, { key: "plans", label: "Plan and usage" }, { key: "team", label: "Users" }] },
  { label: "Access", items: [{ key: "access-control", label: "Roles and permissions" }] },
  { label: "Personal", items: [{ key: "account", label: "My account" }, { key: "notifications", label: "Notifications" }, { key: "security", label: "Security" }] },
];

export function PlatformHome({ user, organization, sessions, permissions, roles, members, invitations, section, settingsSection }: PlatformHomeProps) {
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [availablePermissions, setAvailablePermissions] = useState(permissions);
  const activeSettingsSection = settingsSection ?? (section === "settings" ? "organization" : section);
  const details = sectionDetails[activeSettingsSection];

  async function handleSignOut() {
    setIsSigningOut(true);
    await fetch("/api/auth/logout", { method: "POST" });
    toast.success("You have been signed out.");
    window.location.replace("/login");
  }

  return (
    <SidebarProvider>
      <PlatformSidebar
        activeItem="settings"
        user={user}
        onSignOut={() => void handleSignOut()}
        isSigningOut={isSigningOut}
      />
      <SidebarInset className="min-h-svh bg-background text-foreground">
      <header className="w-full border-b border-border py-4">
        <div className="flex w-full items-center px-[30px]">
          <SidebarTrigger variant="ghost" aria-label="Toggle sidebar" />
        </div>
      </header>

      <section className="grid w-full items-start gap-8 px-[30px] pb-[34px] pt-[22px] lg:grid-cols-[12rem_minmax(0,1fr)] lg:gap-10">
        <nav aria-label="Settings" className="space-y-1">
          <h1 className="sr-only">Settings</h1>
          {settingsNavigation.map((group) => (
            <div key={group.label} className="pb-4">
              <p className="px-3 pb-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">{group.label}</p>
              {group.items.map((item) => (
                <Button
                  key={item.key}
                  variant={activeSettingsSection === item.key ? "secondary" : "ghost"}
                  className="w-full justify-start"
                  type="button"
                  render={<Link href={`/app/${item.key}`} />}
                >
                  {item.label}
                </Button>
              ))}
            </div>
          ))}
        </nav>

        <div className="space-y-6">
        <div>
          <p className="text-sm font-medium text-muted-foreground">{details.eyebrow}</p>
          <h2 className="mt-2 text-2xl font-semibold tracking-tight">{details.title}</h2>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">{details.description}</p>
        </div>
        {activeSettingsSection === "organization" && <OrganizationCard organization={organization} />}
        {activeSettingsSection === "plans" && <PlansCard />}
        {activeSettingsSection === "team" && <MembersCard initialMembers={members} roles={roles} canManage={organization.role === "owner"} />}
        {activeSettingsSection === "team" && <InvitationsCard initialInvitations={invitations} roles={roles} canManage={organization.role === "owner"} />}
        {activeSettingsSection === "access-control" && <PermissionsCard
          initialPermissions={availablePermissions}
          canManage={organization.role === "owner"}
          onPermissionsChange={setAvailablePermissions}
        />}
        {activeSettingsSection === "access-control" && <RolesCard initialRoles={roles} permissions={availablePermissions} canManage={organization.role === "owner"} />}
        {activeSettingsSection === "account" && <AccountSettings user={user} />}
        {activeSettingsSection === "notifications" && <NotificationsCard />}
        {activeSettingsSection === "security" && organization.role === "owner" && <AuditsCard key={organization.id} members={members} />}
        {activeSettingsSection === "security" && <SessionsCard initialSessions={sessions} />}
        </div>
      </section>
      </SidebarInset>
    </SidebarProvider>
  );
}

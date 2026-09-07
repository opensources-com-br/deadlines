"use client";

import { AuditsCard } from "@/features/audits/presentation/AuditsCard";

import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { SidebarInset, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import type { UserSession } from "@/features/platform/domain/session";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { changePassword, updateUserProfile } from "@/features/platform/infrastructure/profile-api";
import { SessionsCard } from "@/features/platform/presentation/SessionsCard";
import { PlatformSidebar, type PlatformNavigationItem } from "@/features/platform/presentation/PlatformSidebar";
import type { Organization } from "@/features/organizations/domain/organization";
import { OrganizationCard } from "@/features/organizations/presentation/OrganizationCard";
import type { Permission, Role } from "@/features/access/domain/access";
import { PermissionsCard } from "@/features/access/presentation/PermissionsCard";
import { RolesCard } from "@/features/access/presentation/RolesCard";
import type { OrganizationInvitation, OrganizationMember } from "@/features/team/domain/team";
import { InvitationsCard } from "@/features/team/presentation/InvitationsCard";
import { MembersCard } from "@/features/team/presentation/MembersCard";
import { SubscriptionCard } from "@/features/subscriptions/presentation/SubscriptionCard";

type PlatformHomeProps = {
  user: UserProfile;
  organization: Organization;
  sessions: UserSession[];
  permissions: Permission[];
  roles: Role[];
  members: OrganizationMember[];
  invitations: OrganizationInvitation[];
  section: PlatformNavigationItem;
};

const sectionDetails: Record<PlatformNavigationItem, { eyebrow: string; title: string; description: string }> = {
  organization: { eyebrow: "Workspace", title: "Organization", description: "Manage your organization and its workspace details." },
  plans: { eyebrow: "Workspace", title: "Plans", description: "Review your organization’s current subscription." },
  team: { eyebrow: "Management", title: "Team", description: "Manage members and invitations for your organization." },
  "access-control": { eyebrow: "Management", title: "Access control", description: "Manage roles and permissions for your organization." },
  security: { eyebrow: "Security", title: "Security", description: "Review organization history and active sessions." },
  account: { eyebrow: "Account", title: "Your account", description: "Manage your personal information and password." },
};

export function PlatformHome({ user, organization, sessions, permissions, roles, members, invitations, section }: PlatformHomeProps) {
  const router = useRouter();
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [profile, setProfile] = useState(user.profile);
  const [firstName, setFirstName] = useState(user.profile.firstName);
  const [lastName, setLastName] = useState(user.profile.lastName);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [availablePermissions, setAvailablePermissions] = useState(permissions);
  const details = sectionDetails[section];

  async function handleSignOut() {
    setIsSigningOut(true);
    await fetch("/api/auth/logout", { method: "POST" });
    toast.success("You have been signed out.");
    router.replace("/login");
    router.refresh();
  }

  async function handleProfileUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);

    try {
      const updated = await updateUserProfile({ firstName, lastName });
      setProfile(updated.profile);
      setFirstName(updated.profile.firstName);
      setLastName(updated.profile.lastName);
      setIsEditing(false);
      toast.success("Your profile has been updated.");
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to update your profile.");
    } finally {
      setIsSaving(false);
    }
  }

  function handleCancelEditing() {
    setFirstName(profile.firstName);
    setLastName(profile.lastName);
    setIsEditing(false);
  }

  async function handlePasswordChange(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (newPassword !== passwordConfirmation) {
      toast.error("Passwords do not match.");
      return;
    }

    setIsSaving(true);
    try {
      await changePassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setPasswordConfirmation("");
      setIsChangingPassword(false);
      toast.success("Your password has been changed.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to change your password.");
    } finally {
      setIsSaving(false);
    }
  }

  function handleCancelPasswordChange() {
    setCurrentPassword("");
    setNewPassword("");
    setPasswordConfirmation("");
    setIsChangingPassword(false);
  }

  return (
    <SidebarProvider>
      <PlatformSidebar user={user} activeItem={section} />
      <SidebarInset className="min-h-svh bg-background text-foreground">
      <header className="w-full py-4">
        <div className="flex w-full items-center justify-between px-[30px]">
          <SidebarTrigger variant="ghost" size="icon-sm" aria-label="Toggle sidebar" />
          <Button variant="outline" size="sm" type="button" onClick={handleSignOut} disabled={isSigningOut}>
            {isSigningOut ? "Signing out..." : "Log out"}
          </Button>
        </div>
      </header>

      <section className="grid w-full items-start gap-8 px-[30px] pb-[34px] pt-[22px] lg:grid-cols-[minmax(16rem,0.65fr)_minmax(0,1fr)] lg:gap-10">
        <div className="max-w-xl">
          <p className="text-sm font-medium text-muted-foreground">{details.eyebrow}</p>
          <h1 className="mt-4 text-4xl font-semibold tracking-tight sm:text-5xl">
            {details.title}
          </h1>
          <p className="mt-5 text-lg leading-8 text-muted-foreground">
            {details.description}
          </p>
        </div>

        <div className="space-y-6">
        {section === "organization" && <OrganizationCard organization={organization} />}
        {section === "plans" && <SubscriptionCard />}
        {section === "team" && <MembersCard initialMembers={members} roles={roles} canManage={organization.role === "owner"} />}
        {section === "team" && <InvitationsCard initialInvitations={invitations} roles={roles} canManage={organization.role === "owner"} />}
        {section === "access-control" && <PermissionsCard
          initialPermissions={availablePermissions}
          canManage={organization.role === "owner"}
          onPermissionsChange={setAvailablePermissions}
        />}
        {section === "access-control" && <RolesCard initialRoles={roles} permissions={availablePermissions} canManage={organization.role === "owner"} />}
        {section === "account" && <Card>
          <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
            <div>
              <CardTitle>Your account</CardTitle>
              <CardDescription className="mt-1">Manage your personal information and password.</CardDescription>
            </div>
            {!isEditing && !isChangingPassword ? (
              <Button variant="outline" type="button" onClick={() => setIsEditing(true)}>
                Edit profile
              </Button>
            ) : null}
          </CardHeader>
          <CardContent>
            {isEditing ? (
              <form onSubmit={handleProfileUpdate}>
                <FieldGroup>
                  <div className="grid gap-6 sm:grid-cols-2">
                    <Field>
                      <FieldLabel htmlFor="profile-first-name">First name</FieldLabel>
                      <Input
                        id="profile-first-name"
                        value={firstName}
                        onChange={(event) => setFirstName(event.target.value)}
                        maxLength={100}
                        required
                      />
                    </Field>
                    <Field>
                      <FieldLabel htmlFor="profile-last-name">Last name</FieldLabel>
                      <Input
                        id="profile-last-name"
                        value={lastName}
                        onChange={(event) => setLastName(event.target.value)}
                        maxLength={100}
                        required
                      />
                    </Field>
                  </div>
                  <Field>
                    <FieldLabel htmlFor="profile-email">Email</FieldLabel>
                    <Input id="profile-email" type="email" value={user.email} disabled />
                  </Field>
                  <Field orientation="horizontal" className="justify-end">
                    <Button variant="outline" type="button" onClick={handleCancelEditing} disabled={isSaving}>
                      Cancel
                    </Button>
                    <Button type="submit" disabled={isSaving}>
                      {isSaving ? "Saving..." : "Save changes"}
                    </Button>
                  </Field>
                </FieldGroup>
              </form>
            ) : isChangingPassword ? (
              <form onSubmit={handlePasswordChange}>
                <FieldGroup>
                  <Field>
                    <FieldLabel htmlFor="current-password">Current password</FieldLabel>
                    <Input
                      id="current-password"
                      type="password"
                      autoComplete="current-password"
                      value={currentPassword}
                      onChange={(event) => setCurrentPassword(event.target.value)}
                      required
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="new-password">New password</FieldLabel>
                    <Input
                      id="new-password"
                      type="password"
                      autoComplete="new-password"
                      value={newPassword}
                      onChange={(event) => setNewPassword(event.target.value)}
                      minLength={12}
                      maxLength={72}
                      required
                    />
                    <p className="text-sm text-muted-foreground">Use between 12 and 72 characters.</p>
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="password-confirmation">Confirm new password</FieldLabel>
                    <Input
                      id="password-confirmation"
                      type="password"
                      autoComplete="new-password"
                      value={passwordConfirmation}
                      onChange={(event) => setPasswordConfirmation(event.target.value)}
                      minLength={12}
                      maxLength={72}
                      required
                    />
                  </Field>
                  <Field orientation="horizontal" className="justify-end">
                    <Button variant="outline" type="button" onClick={handleCancelPasswordChange} disabled={isSaving}>
                      Cancel
                    </Button>
                    <Button type="submit" disabled={isSaving}>
                      {isSaving ? "Changing..." : "Change password"}
                    </Button>
                  </Field>
                </FieldGroup>
              </form>
            ) : (
              <div className="space-y-6">
                <dl className="space-y-5">
                  <div className="grid gap-1 sm:grid-cols-[140px_1fr] sm:gap-6">
                    <dt className="text-sm text-muted-foreground">Full name</dt>
                    <dd className="text-sm font-medium">{profile.firstName} {profile.lastName}</dd>
                  </div>
                  <div className="grid gap-1 sm:grid-cols-[140px_1fr] sm:gap-6">
                    <dt className="text-sm text-muted-foreground">Email</dt>
                    <dd className="text-sm font-medium">{user.email}</dd>
                  </div>
                </dl>
                <Separator />
                <div className="flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-center">
                  <div>
                    <p className="text-sm font-medium">Password</p>
                    <p className="mt-1 text-sm text-muted-foreground">Update your password without leaving your account.</p>
                  </div>
                  <Button variant="outline" type="button" onClick={() => setIsChangingPassword(true)}>
                    Change password
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>}
        {section === "security" && organization.role === "owner" && <AuditsCard key={organization.id} members={members} />}
        {section === "security" && <SessionsCard initialSessions={sessions} />}
        </div>
      </section>
      </SidebarInset>
    </SidebarProvider>
  );
}

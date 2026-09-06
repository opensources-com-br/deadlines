"use client";

import { type FormEvent, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import type { Role } from "@/features/access/domain/access";
import type { OrganizationInvitation } from "@/features/team/domain/team";
import { teamApi } from "@/features/team/infrastructure/team-api";

type InvitationsCardProps = {
  initialInvitations: OrganizationInvitation[];
  roles: Role[];
  canManage: boolean;
};

export function InvitationsCard({ initialInvitations, roles, canManage }: InvitationsCardProps) {
  const [invitations, setInvitations] = useState(() => initialInvitations.filter((invitation) => invitation.status !== "revoked"));
  const [isCreating, setIsCreating] = useState(false);
  const [email, setEmail] = useState("");
  const [roleId, setRoleId] = useState(roles.find((role) => role.key === "member")?.id ?? "");
  const [busyId, setBusyId] = useState<string>();
  const assignableRoles = roles.filter((role) => role.key !== "owner");

  async function createInvitation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusyId("new");
    try {
      const invitation = await teamApi.createInvitation(email, roleId);
      setInvitations((current) => [invitation, ...current]);
      setEmail("");
      setIsCreating(false);
      toast.success("Invitation sent.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to send this invitation.");
    } finally {
      setBusyId(undefined);
    }
  }

  async function resend(invitation: OrganizationInvitation) {
    setBusyId(invitation.id);
    try {
      const updated = await teamApi.resendInvitation(invitation.id);
      setInvitations((current) => current.map((item) => item.id === updated.id ? updated : item));
      toast.success("Invitation sent again.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to resend this invitation.");
    } finally {
      setBusyId(undefined);
    }
  }

  async function revoke(invitation: OrganizationInvitation) {
    if (!window.confirm(`Revoke the invitation for ${invitation.email}?`)) return;
    setBusyId(invitation.id);
    try {
      await teamApi.revokeInvitation(invitation.id);
      setInvitations((current) => current.filter((item) => item.id !== invitation.id));
      toast.success("Invitation revoked.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to revoke this invitation.");
    } finally {
      setBusyId(undefined);
    }
  }

  return (
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
        <div>
          <CardTitle>Invitations</CardTitle>
          <CardDescription className="mt-1">Invite people and choose their initial role.</CardDescription>
        </div>
        {canManage && !isCreating ? <Button variant="outline" type="button" onClick={() => setIsCreating(true)}>Invite member</Button> : null}
      </CardHeader>
      <CardContent>
        {isCreating ? (
          <form onSubmit={createInvitation}>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="invitation-email">Email</FieldLabel>
                <Input id="invitation-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
              </Field>
              <Field>
                <FieldLabel>Role</FieldLabel>
                <Select value={roleId} onValueChange={(value) => setRoleId(value ?? "")} required>
                  <SelectTrigger className="w-full">
                    <SelectValue>{assignableRoles.find((role) => role.id === roleId)?.name ?? "Select a role"}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {assignableRoles.map((role) => <SelectItem key={role.id} value={role.id}>{role.name}</SelectItem>)}
                  </SelectContent>
                </Select>
              </Field>
              <Field orientation="horizontal" className="justify-end">
                <Button variant="outline" type="button" onClick={() => setIsCreating(false)} disabled={busyId === "new"}>Cancel</Button>
                <Button type="submit" disabled={busyId === "new" || !roleId}>{busyId === "new" ? "Sending..." : "Send invitation"}</Button>
              </Field>
            </FieldGroup>
          </form>
        ) : invitations.length === 0 ? (
          <p className="text-sm text-muted-foreground">No invitations yet.</p>
        ) : (
          <div className="space-y-4">
            {invitations.map((invitation, index) => {
              const actionable = invitation.status === "pending" || invitation.status === "expired";
              return (
                <div key={invitation.id}>
                  {index > 0 ? <Separator className="mb-4" /> : null}
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{invitation.email}</p>
                      <p className="mt-1 text-xs text-muted-foreground">{invitation.role.name} · {invitation.status}</p>
                    </div>
                    {canManage && actionable ? (
                      <div className="flex gap-1">
                        <Button variant="ghost" size="sm" type="button" onClick={() => resend(invitation)} disabled={busyId === invitation.id}>Resend</Button>
                        <Button variant="ghost" size="sm" type="button" onClick={() => revoke(invitation)} disabled={busyId === invitation.id}>Revoke</Button>
                      </div>
                    ) : null}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

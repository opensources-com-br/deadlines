"use client";

import { type FormEvent, useState } from "react";
import { ChevronDown } from "lucide-react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { DropdownMenu, DropdownMenuContent, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import type { Role } from "@/features/access/domain/access";
import { platformPermission } from "@/features/access/domain/authorization";
import { usePermission } from "@/features/access/presentation/AuthorizationProvider";
import type { OrganizationInvitation } from "@/features/team/domain/team";
import { teamApi } from "@/features/team/infrastructure/team-api";
import { useLocalizedFormatters } from "@/features/platform/presentation/useLocalizedFormatters";

type InvitationsCardProps = {
  initialInvitations: OrganizationInvitation[];
  roles: Role[];
};

export function InvitationsCard({ initialInvitations, roles }: InvitationsCardProps) {
  const t = useTranslations("Team");
  const canInvite = usePermission(platformPermission.membersInvite);
  const { formatDate } = useLocalizedFormatters();
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
      toast.success(t("sent"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("sendError"));
    } finally {
      setBusyId(undefined);
    }
  }

  async function resend(invitation: OrganizationInvitation) {
    setBusyId(invitation.id);
    try {
      const updated = await teamApi.resendInvitation(invitation.id);
      setInvitations((current) => current.map((item) => item.id === updated.id ? updated : item));
      toast.success(t("resent"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("resendError"));
    } finally {
      setBusyId(undefined);
    }
  }

  async function revoke(invitation: OrganizationInvitation) {
    if (!window.confirm(t("revokeConfirm", { email: invitation.email }))) return;
    setBusyId(invitation.id);
    try {
      await teamApi.revokeInvitation(invitation.id);
      setInvitations((current) => current.filter((item) => item.id !== invitation.id));
      toast.success(t("revokedMessage"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("revokeError"));
    } finally {
      setBusyId(undefined);
    }
  }

  return (
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
        <div>
          <CardTitle>{t("invitations")}</CardTitle>
          <CardDescription className="mt-1">{t("invitationsDescription")}</CardDescription>
        </div>
        {canInvite && !isCreating ? <Button variant="outline" type="button" onClick={() => setIsCreating(true)}>{t("invite")}</Button> : null}
      </CardHeader>
      <CardContent>
        {isCreating ? (
          <form onSubmit={createInvitation}>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="invitation-email">{t("email")}</FieldLabel>
                <Input id="invitation-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
              </Field>
              <Field>
                <FieldLabel>{t("role")}</FieldLabel>
                <DropdownMenu>
                  <DropdownMenuTrigger render={<Button variant="outline" className="w-full justify-between" />}>
                    {assignableRoles.find((role) => role.id === roleId)?.name ?? t("selectRole")}
                    <ChevronDown />
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="start" className="min-w-(--anchor-width)">
                    <DropdownMenuRadioGroup value={roleId} onValueChange={(value) => setRoleId(value)}>
                      {assignableRoles.map((role) => <DropdownMenuRadioItem key={role.id} value={role.id}>{role.name}</DropdownMenuRadioItem>)}
                    </DropdownMenuRadioGroup>
                  </DropdownMenuContent>
                </DropdownMenu>
              </Field>
              <Field orientation="horizontal" className="justify-end">
                <Button variant="outline" type="button" onClick={() => setIsCreating(false)} disabled={busyId === "new"}>{t("cancel")}</Button>
                <Button type="submit" disabled={busyId === "new" || !roleId}>{busyId === "new" ? t("sending") : t("send")}</Button>
              </Field>
            </FieldGroup>
          </form>
        ) : invitations.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("none")}</p>
        ) : (
          <div className="overflow-x-auto rounded-lg border">
          <table className="w-full min-w-[600px] text-left text-sm">
            <thead className="border-b bg-muted/40 text-xs font-medium uppercase tracking-wide text-muted-foreground"><tr><th className="px-4 py-3">{t("email")}</th><th className="px-4 py-3">{t("role")}</th><th className="px-4 py-3">{t("status")}</th><th className="px-4 py-3">{t("expires")}</th><th className="px-4 py-3 text-right"><span className="sr-only">{t("actions")}</span></th></tr></thead>
            <tbody className="divide-y">
            {invitations.map((invitation) => {
              const actionable = invitation.status === "pending" || invitation.status === "expired";
              return (
                <tr key={invitation.id}>
                    <td className="px-4 py-3 font-medium">{invitation.email}</td>
                    <td className="px-4 py-3 text-muted-foreground">{invitation.role.name}</td>
                    <td className="px-4 py-3"><span className="rounded-full bg-amber-500/10 px-2.5 py-1 text-xs font-medium text-amber-700 dark:text-amber-400">{t(invitation.status as "pending" | "expired" | "accepted" | "revoked")}</span></td>
                    <td className="px-4 py-3 whitespace-nowrap text-muted-foreground"><time dateTime={invitation.expiresAt}>{formatDate(invitation.expiresAt) ?? "—"}</time></td>
                    <td className="px-4 py-3 text-right">{canInvite && actionable ? (
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" type="button" onClick={() => resend(invitation)} disabled={busyId === invitation.id}>{t("resend")}</Button>
                        <Button variant="ghost" size="sm" type="button" onClick={() => revoke(invitation)} disabled={busyId === invitation.id}>{t("revoke")}</Button>
                      </div>) : null}</td>
                </tr>
              );
            })}
            </tbody>
          </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

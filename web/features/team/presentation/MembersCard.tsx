"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { DropdownMenu, DropdownMenuContent, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import type { Role } from "@/features/access/domain/access";
import { platformPermission } from "@/features/access/domain/authorization";
import { usePermission } from "@/features/access/presentation/AuthorizationProvider";
import type { OrganizationMember } from "@/features/team/domain/team";
import { teamApi } from "@/features/team/infrastructure/team-api";
import { useLocalizedFormatters } from "@/features/platform/presentation/useLocalizedFormatters";

type MembersCardProps = {
  initialMembers: OrganizationMember[];
  roles: Role[];
};

export function MembersCard({ initialMembers, roles }: MembersCardProps) {
  const t = useTranslations("Team");
  const canUpdate = usePermission(platformPermission.membersUpdate);
  const canRemove = usePermission(platformPermission.membersRemove);
  const { formatDate } = useLocalizedFormatters();
  const [members, setMembers] = useState(initialMembers);
  const [busyMemberId, setBusyMemberId] = useState<string>();
  const assignableRoles = roles.filter((role) => role.key !== "owner");

  async function changeRole(member: OrganizationMember, roleId: string | null) {
    if (!roleId || roleId === member.role.id) return;
    setBusyMemberId(member.id);
    try {
      const updated = await teamApi.updateMemberRole(member.id, roleId);
      setMembers((current) => current.map((item) => item.id === updated.id ? updated : item));
      toast.success(t("roleUpdated"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("updateError"));
    } finally {
      setBusyMemberId(undefined);
    }
  }

  async function removeMember(member: OrganizationMember) {
    if (!window.confirm(t("removeConfirm", { name: `${member.firstName} ${member.lastName}` }))) return;
    setBusyMemberId(member.id);
    try {
      await teamApi.removeMember(member.id);
      setMembers((current) => current.filter((item) => item.id !== member.id));
      toast.success(t("removed"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("removeError"));
    } finally {
      setBusyMemberId(undefined);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("users")}</CardTitle>
        <CardDescription className="mt-1">{t("usersDescription")}</CardDescription>
      </CardHeader>
      <CardContent>
        {members.length === 0 ? <p className="text-sm text-muted-foreground">{t("noUsers")}</p> : <div className="overflow-x-auto rounded-lg border">
          <table className="w-full min-w-[680px] text-left text-sm">
            <thead className="border-b bg-muted/40 text-xs font-medium uppercase tracking-wide text-muted-foreground">
              <tr><th className="px-4 py-3">{t("user")}</th><th className="px-4 py-3">{t("role")}</th><th className="px-4 py-3">{t("status")}</th><th className="px-4 py-3">{t("joined")}</th><th className="px-4 py-3 text-right"><span className="sr-only">{t("actions")}</span></th></tr>
            </thead>
            <tbody className="divide-y">
          {members.map((member) => {
            const isOwner = member.role.key === "owner";
            return (
              <tr key={member.id}>
                  <td className="px-4 py-3"><div className="flex min-w-0 items-center gap-3"><Avatar className="size-8"><AvatarFallback>{`${member.firstName[0] ?? ""}${member.lastName[0] ?? ""}`.toUpperCase()}</AvatarFallback></Avatar><div className="min-w-0"><p className="truncate font-medium">{member.firstName} {member.lastName}</p><p className="truncate text-xs text-muted-foreground">{member.email}</p></div></div></td>
                  <td className="px-4 py-3">
                    {canUpdate && !isOwner ? (
                      <DropdownMenu>
                        <DropdownMenuTrigger
                          disabled={busyMemberId === member.id}
                          render={<Button variant="outline" size="sm" className="min-w-32 justify-between" />}
                        >
                          {member.role.name}<ChevronDown />
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="start">
                          <DropdownMenuRadioGroup value={member.role.id} onValueChange={(value) => changeRole(member, value)}>
                            {assignableRoles.map((role) => <DropdownMenuRadioItem key={role.id} value={role.id}>{role.name}</DropdownMenuRadioItem>)}
                          </DropdownMenuRadioGroup>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    ) : (
                      <span className="rounded-full border px-2.5 py-1 text-xs text-muted-foreground">{member.role.name}</span>
                    )}
                  </td>
                  <td className="px-4 py-3"><span className="rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-700 dark:text-emerald-400">{t("active")}</span></td>
                  <td className="whitespace-nowrap px-4 py-3 text-muted-foreground"><time dateTime={member.joinedAt}>{formatDate(member.joinedAt) ?? "—"}</time></td>
                  <td className="px-4 py-3 text-right">{canRemove && !isOwner ? <Button variant="ghost" size="sm" type="button" onClick={() => removeMember(member)} disabled={busyMemberId === member.id}>{t("remove")}</Button> : null}</td>
              </tr>
            );
          })}
            </tbody>
          </table>
        </div>}
      </CardContent>
    </Card>
  );
}

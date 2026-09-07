"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { DropdownMenu, DropdownMenuContent, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import type { Role } from "@/features/access/domain/access";
import type { OrganizationMember } from "@/features/team/domain/team";
import { teamApi } from "@/features/team/infrastructure/team-api";

type MembersCardProps = {
  initialMembers: OrganizationMember[];
  roles: Role[];
  canManage: boolean;
};

export function MembersCard({ initialMembers, roles, canManage }: MembersCardProps) {
  const [members, setMembers] = useState(initialMembers);
  const [busyMemberId, setBusyMemberId] = useState<string>();
  const assignableRoles = roles.filter((role) => role.key !== "owner");
  const formatter = new Intl.DateTimeFormat("en", { month: "short", day: "numeric", year: "numeric" });

  async function changeRole(member: OrganizationMember, roleId: string | null) {
    if (!roleId || roleId === member.role.id) return;
    setBusyMemberId(member.id);
    try {
      const updated = await teamApi.updateMemberRole(member.id, roleId);
      setMembers((current) => current.map((item) => item.id === updated.id ? updated : item));
      toast.success("Member role updated.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to update this member.");
    } finally {
      setBusyMemberId(undefined);
    }
  }

  async function removeMember(member: OrganizationMember) {
    if (!window.confirm(`Remove ${member.firstName} ${member.lastName} from the organization?`)) return;
    setBusyMemberId(member.id);
    try {
      await teamApi.removeMember(member.id);
      setMembers((current) => current.filter((item) => item.id !== member.id));
      toast.success("Member removed.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to remove this member.");
    } finally {
      setBusyMemberId(undefined);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Users</CardTitle>
        <CardDescription className="mt-1">Manage the people who currently have access to this organization.</CardDescription>
      </CardHeader>
      <CardContent>
        {members.length === 0 ? <p className="text-sm text-muted-foreground">No users have joined this organization yet.</p> : <div className="overflow-x-auto rounded-lg border">
          <table className="w-full min-w-[680px] text-left text-sm">
            <thead className="border-b bg-muted/40 text-xs font-medium uppercase tracking-wide text-muted-foreground">
              <tr><th className="px-4 py-3">User</th><th className="px-4 py-3">Role</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Joined</th><th className="px-4 py-3 text-right"><span className="sr-only">Actions</span></th></tr>
            </thead>
            <tbody className="divide-y">
          {members.map((member) => {
            const isOwner = member.role.key === "owner";
            return (
              <tr key={member.id}>
                  <td className="px-4 py-3"><div className="flex min-w-0 items-center gap-3"><Avatar className="size-8"><AvatarFallback>{`${member.firstName[0] ?? ""}${member.lastName[0] ?? ""}`.toUpperCase()}</AvatarFallback></Avatar><div className="min-w-0"><p className="truncate font-medium">{member.firstName} {member.lastName}</p><p className="truncate text-xs text-muted-foreground">{member.email}</p></div></div></td>
                  <td className="px-4 py-3">
                    {canManage && !isOwner ? (
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
                  <td className="px-4 py-3"><span className="rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-700 dark:text-emerald-400">Active</span></td>
                  <td className="whitespace-nowrap px-4 py-3 text-muted-foreground"><time dateTime={member.joinedAt}>{formatter.format(new Date(member.joinedAt))}</time></td>
                  <td className="px-4 py-3 text-right">{canManage && !isOwner ? <Button variant="ghost" size="sm" type="button" onClick={() => removeMember(member)} disabled={busyMemberId === member.id}>Remove</Button> : null}</td>
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

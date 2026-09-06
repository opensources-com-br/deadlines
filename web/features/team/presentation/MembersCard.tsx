"use client";

import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
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
        <CardTitle>Members</CardTitle>
        <CardDescription className="mt-1">People with access to this organization.</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {members.map((member, index) => {
            const isOwner = member.role.key === "owner";
            return (
              <div key={member.id}>
                {index > 0 ? <Separator className="mb-4" /> : null}
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{member.firstName} {member.lastName}</p>
                    <p className="truncate text-xs text-muted-foreground">{member.email}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    {canManage && !isOwner ? (
                      <Select
                        value={member.role.id}
                        onValueChange={(value) => changeRole(member, value)}
                        disabled={busyMemberId === member.id}
                      >
                        <SelectTrigger size="sm" className="min-w-32">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {assignableRoles.map((role) => <SelectItem key={role.id} value={role.id}>{role.name}</SelectItem>)}
                        </SelectContent>
                      </Select>
                    ) : (
                      <span className="rounded-full border px-2.5 py-1 text-xs text-muted-foreground">{member.role.name}</span>
                    )}
                    {canManage && !isOwner ? (
                      <Button variant="ghost" size="sm" type="button" onClick={() => removeMember(member)} disabled={busyMemberId === member.id}>
                        Remove
                      </Button>
                    ) : null}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}

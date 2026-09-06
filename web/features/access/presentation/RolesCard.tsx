"use client";

import { type FormEvent, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import type { Permission, Role } from "@/features/access/domain/access";
import { accessApi } from "@/features/access/infrastructure/access-api";

type RolesCardProps = {
  initialRoles: Role[];
  permissions: Permission[];
  canManage: boolean;
};

export function RolesCard({ initialRoles, permissions, canManage }: RolesCardProps) {
  const [roles, setRoles] = useState(initialRoles);
  const [editing, setEditing] = useState<Role | "new">();
  const [key, setKey] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [managingRole, setManagingRole] = useState<Role>();
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<string[]>([]);
  const [loadingRoleId, setLoadingRoleId] = useState<string>();

  function beginEdit(role?: Role) {
    setEditing(role ?? "new");
    setKey(role?.key ?? "");
    setName(role?.name ?? "");
    setDescription(role?.description ?? "");
  }

  function cancelEdit() {
    setEditing(undefined);
    setKey("");
    setName("");
    setDescription("");
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    try {
      const input = { key, name, description: description || undefined };
      const saved = editing === "new"
        ? await accessApi.createRole(input)
        : await accessApi.updateRole(editing!.id, input);
      setRoles((current) =>
        editing === "new" ? [...current, saved] : current.map((item) => item.id === saved.id ? saved : item),
      );
      cancelEdit();
      toast.success(editing === "new" ? "Role created." : "Role updated.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to save this role.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(role: Role) {
    if (!window.confirm(`Delete the “${role.name}” role?`)) return;
    try {
      await accessApi.deleteRole(role.id);
      setRoles((current) => current.filter((item) => item.id !== role.id));
      toast.success("Role deleted.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to delete this role.");
    }
  }

  async function beginManagePermissions(role: Role) {
    setLoadingRoleId(role.id);
    try {
      const assigned = await accessApi.listRolePermissions(role.id);
      setManagingRole(role);
      setSelectedPermissionIds(assigned.data.map((permission) => permission.id));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to load role permissions.");
    } finally {
      setLoadingRoleId(undefined);
    }
  }

  async function savePermissions() {
    if (!managingRole) return;
    setIsSaving(true);
    try {
      await accessApi.replaceRolePermissions(managingRole.id, selectedPermissionIds);
      toast.success("Role permissions updated.");
      setManagingRole(undefined);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to update role permissions.");
    } finally {
      setIsSaving(false);
    }
  }

  function togglePermission(permissionId: string, checked: boolean) {
    setSelectedPermissionIds((current) =>
      checked ? [...new Set([...current, permissionId])] : current.filter((id) => id !== permissionId),
    );
  }

  return (
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
        <div>
          <CardTitle>Roles</CardTitle>
          <CardDescription className="mt-1">Group permissions into roles for future team members.</CardDescription>
        </div>
        {canManage && !editing ? <Button variant="outline" type="button" onClick={() => beginEdit()}>New role</Button> : null}
      </CardHeader>
      <CardContent>
        {managingRole ? (
          <div className="space-y-5">
            <div>
              <p className="text-sm font-medium">{managingRole.name}</p>
              <p className="mt-1 text-xs text-muted-foreground">
                {managingRole.key === "owner" ? "The owner always retains every permission." : "Select the capabilities assigned to this role."}
              </p>
            </div>
            <div className="space-y-3">
              {permissions.map((permission) => (
                <label key={permission.id} className="flex cursor-pointer items-start gap-3 rounded-lg border p-3">
                  <Checkbox
                    className="mt-0.5"
                    checked={selectedPermissionIds.includes(permission.id)}
                    onCheckedChange={(checked) => togglePermission(permission.id, checked === true)}
                    disabled={!canManage || managingRole.key === "owner"}
                  />
                  <span className="min-w-0">
                    <span className="block text-sm font-medium">{permission.name}</span>
                    <span className="block font-mono text-xs text-muted-foreground">{permission.key}</span>
                  </span>
                </label>
              ))}
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" type="button" onClick={() => setManagingRole(undefined)} disabled={isSaving}>Back</Button>
              {canManage && managingRole.key !== "owner" ? (
                <Button type="button" onClick={savePermissions} disabled={isSaving}>{isSaving ? "Saving..." : "Save permissions"}</Button>
              ) : null}
            </div>
          </div>
        ) : editing ? (
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="role-name">Name</FieldLabel>
                <Input id="role-name" value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required />
              </Field>
              <Field>
                <FieldLabel htmlFor="role-key">Key</FieldLabel>
                <Input id="role-key" value={key} onChange={(event) => setKey(event.target.value.toLowerCase())} maxLength={80} placeholder="project-manager" required />
              </Field>
              <Field>
                <FieldLabel htmlFor="role-description">Description</FieldLabel>
                <Input id="role-description" value={description} onChange={(event) => setDescription(event.target.value)} maxLength={500} />
              </Field>
              <Field orientation="horizontal" className="justify-end">
                <Button variant="outline" type="button" onClick={cancelEdit} disabled={isSaving}>Cancel</Button>
                <Button type="submit" disabled={isSaving}>{isSaving ? "Saving..." : "Save role"}</Button>
              </Field>
            </FieldGroup>
          </form>
        ) : (
          <div className="space-y-4">
            {roles.map((role, index) => (
              <div key={role.id}>
                {index > 0 ? <Separator className="mb-4" /> : null}
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-medium">{role.name}</p>
                      {role.isSystem ? <span className="rounded-full border px-2 py-0.5 text-xs text-muted-foreground">System</span> : null}
                    </div>
                    <p className="mt-1 font-mono text-xs text-muted-foreground">{role.key}</p>
                    {role.description ? <p className="mt-1 text-xs text-muted-foreground">{role.description}</p> : null}
                  </div>
                  <div className="flex flex-wrap justify-end gap-1">
                    <Button variant="ghost" size="sm" type="button" onClick={() => beginManagePermissions(role)} disabled={loadingRoleId === role.id}>
                      {loadingRoleId === role.id ? "Loading..." : "Permissions"}
                    </Button>
                    {canManage && !role.isSystem ? (
                      <>
                        <Button variant="ghost" size="sm" type="button" onClick={() => beginEdit(role)}>Edit</Button>
                        <Button variant="ghost" size="sm" type="button" onClick={() => handleDelete(role)}>Delete</Button>
                      </>
                    ) : null}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

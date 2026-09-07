"use client";

import { type FormEvent, useState } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import type { Permission, Role } from "@/features/access/domain/access";
import { platformPermission } from "@/features/access/domain/authorization";
import { accessApi } from "@/features/access/infrastructure/access-api";
import { usePermission } from "@/features/access/presentation/AuthorizationProvider";

type RolesCardProps = {
  initialRoles: Role[];
  permissions: Permission[];
};

export function RolesCard({ initialRoles, permissions }: RolesCardProps) {
  const t = useTranslations("AccessControl");
  const canCreate = usePermission(platformPermission.rolesCreate);
  const canUpdate = usePermission(platformPermission.rolesUpdate);
  const canDelete = usePermission(platformPermission.rolesDelete);
  const canReadPermissions = usePermission(platformPermission.permissionsRead);
  const canManagePermissions = canUpdate && canReadPermissions;
  const [roles, setRoles] = useState(initialRoles);
  const [editing, setEditing] = useState<Role | "new">();
  const [key, setKey] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [managingRole, setManagingRole] = useState<Role>();
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<string[]>([]);
  const [loadingRoleId, setLoadingRoleId] = useState<string>();
  const permissionGroups = Object.entries(permissions.reduce<Record<string, Permission[]>>((groups, permission) => {
    const key = permission.key.split(".")[0] || "general";
    groups[key] = [...(groups[key] ?? []), permission];
    return groups;
  }, {}));

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
      const input = { key, name, description };
      const saved = editing === "new"
        ? await accessApi.createRole(input)
        : await accessApi.updateRole(editing!.id, input);
      setRoles((current) =>
        editing === "new" ? [...current, saved] : current.map((item) => item.id === saved.id ? saved : item),
      );
      cancelEdit();
      toast.success(editing === "new" ? t("roleCreated") : t("roleUpdated"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("roleSaveError"));
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(role: Role) {
    if (!window.confirm(t("roleDeleteConfirm", { name: role.name }))) return;
    try {
      await accessApi.deleteRole(role.id);
      setRoles((current) => current.filter((item) => item.id !== role.id));
      toast.success(t("roleDeleted"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("roleDeleteError"));
    }
  }

  async function beginManagePermissions(role: Role) {
    setLoadingRoleId(role.id);
    try {
      const assigned = await accessApi.listRolePermissions(role.id);
      setManagingRole(role);
      setSelectedPermissionIds(assigned.data.map((permission) => permission.id));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("permissionsLoadError"));
    } finally {
      setLoadingRoleId(undefined);
    }
  }

  async function savePermissions() {
    if (!managingRole) return;
    setIsSaving(true);
    try {
      await accessApi.replaceRolePermissions(managingRole.id, selectedPermissionIds);
      toast.success(t("permissionsUpdated"));
      setManagingRole(undefined);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("permissionsUpdateError"));
    } finally {
      setIsSaving(false);
    }
  }

  function togglePermission(permissionId: string, checked: boolean) {
    setSelectedPermissionIds((current) =>
      checked ? [...new Set([...current, permissionId])] : current.filter((id) => id !== permissionId),
    );
  }

  function togglePermissionGroup(groupPermissions: Permission[], checked: boolean) {
    const ids = groupPermissions.map((permission) => permission.id);
    setSelectedPermissionIds((current) => checked ? [...new Set([...current, ...ids])] : current.filter((id) => !ids.includes(id)));
  }

  return (
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
        <div>
          <CardTitle>{t("roles")}</CardTitle><CardDescription className="mt-1">{t("rolesDescription")}</CardDescription>
        </div>
        {canCreate && !editing ? <Button variant="outline" type="button" onClick={() => beginEdit()}>{t("newRole")}</Button> : null}
      </CardHeader>
      <CardContent>
        {managingRole ? (
          <div className="space-y-5">
            <div>
              <p className="text-sm font-medium">{managingRole.name}</p>
              <p className="mt-1 text-xs text-muted-foreground">
                {managingRole.key === "owner" ? t("ownerPermissions") : t("selectCapabilities")}
              </p>
            </div>
            <div className="space-y-5">
              {permissionGroups.map(([group, groupPermissions]) => {
                const allSelected = groupPermissions.every((permission) => selectedPermissionIds.includes(permission.id));
                return <section key={group} className="rounded-lg border">
                  <div className="flex items-center justify-between gap-4 border-b bg-muted/30 px-4 py-3">
                    <div><h4 className="text-sm font-medium capitalize">{group}</h4><p className="text-xs text-muted-foreground">{t("permissionCount", { count: groupPermissions.length })}</p></div>
                    <label className="flex cursor-pointer items-center gap-2 text-xs text-muted-foreground"><Checkbox checked={allSelected} onCheckedChange={(checked) => togglePermissionGroup(groupPermissions, checked === true)} disabled={!canManagePermissions || managingRole.key === "owner"} />{t("selectAll")}</label>
                  </div>
                  <div className="divide-y">
                    {groupPermissions.map((permission) => (
                      <label key={permission.id} className="flex cursor-pointer items-start gap-3 px-4 py-3">
                        <Checkbox className="mt-0.5" checked={selectedPermissionIds.includes(permission.id)} onCheckedChange={(checked) => togglePermission(permission.id, checked === true)} disabled={!canManagePermissions || managingRole.key === "owner"} />
                        <span className="min-w-0"><span className="block text-sm font-medium">{permission.name}</span><span className="block font-mono text-xs text-muted-foreground">{permission.key}</span>{permission.description ? <span className="mt-1 block text-xs text-muted-foreground">{permission.description}</span> : null}</span>
                      </label>
                    ))}
                  </div>
                </section>;
              })}
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" type="button" onClick={() => setManagingRole(undefined)} disabled={isSaving}>{t("back")}</Button>
              {canManagePermissions && managingRole.key !== "owner" ? (
                <Button type="button" onClick={savePermissions} disabled={isSaving}>{isSaving ? t("saving") : t("savePermissions")}</Button>
              ) : null}
            </div>
          </div>
        ) : editing ? (
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="role-name">{t("name")}</FieldLabel>
                <Input id="role-name" value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required />
              </Field>
              <Field>
                <FieldLabel htmlFor="role-key">{t("key")}</FieldLabel>
                <Input id="role-key" value={key} onChange={(event) => setKey(event.target.value.toLowerCase())} maxLength={80} placeholder="project-manager" required />
              </Field>
              <Field>
                <FieldLabel htmlFor="role-description">{t("description")}</FieldLabel>
                <Input id="role-description" value={description} onChange={(event) => setDescription(event.target.value)} maxLength={500} />
              </Field>
              <Field orientation="horizontal" className="justify-end">
                <Button variant="outline" type="button" onClick={cancelEdit} disabled={isSaving}>{t("cancel")}</Button><Button type="submit" disabled={isSaving}>{isSaving ? t("saving") : t("saveRole")}</Button>
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
                      {role.isSystem ? <span className="rounded-full border px-2 py-0.5 text-xs text-muted-foreground">{t("system")}</span> : null}
                    </div>
                    <p className="mt-1 font-mono text-xs text-muted-foreground">{role.key}</p>
                    {role.description ? <p className="mt-1 text-xs text-muted-foreground">{role.description}</p> : null}
                  </div>
                  <div className="flex flex-wrap justify-end gap-1">
                    {canReadPermissions ? <Button variant="ghost" size="sm" type="button" onClick={() => beginManagePermissions(role)} disabled={loadingRoleId === role.id}>
                      {loadingRoleId === role.id ? t("loading") : t("permissions")}
                    </Button> : null}
                    {(canUpdate || canDelete) && !role.isSystem ? (
                      <>
                        {canUpdate ? <Button variant="ghost" size="sm" type="button" onClick={() => beginEdit(role)}>{t("edit")}</Button> : null}
                        {canDelete ? <Button variant="ghost" size="sm" type="button" onClick={() => handleDelete(role)}>{t("delete")}</Button> : null}
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

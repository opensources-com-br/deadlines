"use client";

import { type FormEvent, useState } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import type { Permission } from "@/features/access/domain/access";
import { platformPermission } from "@/features/access/domain/authorization";
import { accessApi } from "@/features/access/infrastructure/access-api";
import { usePermission } from "@/features/access/presentation/AuthorizationProvider";

type PermissionsCardProps = {
  initialPermissions: Permission[];
  onPermissionsChange: (permissions: Permission[]) => void;
};

export function PermissionsCard({ initialPermissions, onPermissionsChange }: PermissionsCardProps) {
  const t = useTranslations("AccessControl");
  const canCreate = usePermission(platformPermission.permissionsCreate);
  const canUpdate = usePermission(platformPermission.permissionsUpdate);
  const canDelete = usePermission(platformPermission.permissionsDelete);
  const [permissions, setPermissions] = useState(initialPermissions);
  const [editing, setEditing] = useState<Permission | "new">();
  const [key, setKey] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const permissionGroups = Object.entries(permissions.reduce<Record<string, Permission[]>>((groups, permission) => {
    const key = permission.key.split(".")[0] || "general";
    groups[key] = [...(groups[key] ?? []), permission];
    return groups;
  }, {}));

  function beginEdit(permission?: Permission) {
    setEditing(permission ?? "new");
    setKey(permission?.key ?? "");
    setName(permission?.name ?? "");
    setDescription(permission?.description ?? "");
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
        ? await accessApi.createPermission(input)
        : await accessApi.updatePermission(editing!.id, input);
      const updated = editing === "new"
        ? [...permissions, saved]
        : permissions.map((item) => item.id === saved.id ? saved : item);
      setPermissions(updated);
      onPermissionsChange(updated);
      cancelEdit();
      toast.success(editing === "new" ? t("permissionCreated") : t("permissionUpdated"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("permissionSaveError"));
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(permission: Permission) {
    if (!window.confirm(t("permissionDeleteConfirm", { name: permission.name }))) return;
    try {
      await accessApi.deletePermission(permission.id);
      const updated = permissions.filter((item) => item.id !== permission.id);
      setPermissions(updated);
      onPermissionsChange(updated);
      toast.success(t("permissionDeleted"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("permissionDeleteError"));
    }
  }

  return (
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
        <div>
          <CardTitle>{t("permissions")}</CardTitle><CardDescription className="mt-1">{t("permissionsDescription")}</CardDescription>
        </div>
        {canCreate && !editing ? (
          <Button variant="outline" type="button" onClick={() => beginEdit()}>
            {t("newPermission")}
          </Button>
        ) : null}
      </CardHeader>
      <CardContent>
        {editing ? (
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="permission-name">{t("name")}</FieldLabel>
                <Input id="permission-name" value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required />
              </Field>
              <Field>
                <FieldLabel htmlFor="permission-key">{t("key")}</FieldLabel>
                <Input
                  id="permission-key"
                  value={key}
                  onChange={(event) => setKey(event.target.value.toLowerCase())}
                  maxLength={100}
                  placeholder="deadlines.manage"
                  required
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="permission-description">{t("description")}</FieldLabel>
                <Input id="permission-description" value={description} onChange={(event) => setDescription(event.target.value)} maxLength={500} />
              </Field>
              <Field orientation="horizontal" className="justify-end">
                <Button variant="outline" type="button" onClick={cancelEdit} disabled={isSaving}>{t("cancel")}</Button>
                <Button type="submit" disabled={isSaving}>{isSaving ? t("saving") : t("savePermission")}</Button>
              </Field>
            </FieldGroup>
          </form>
        ) : (
          <div className="space-y-5">
            {permissionGroups.map(([group, groupPermissions]) => (
              <section key={group} className="rounded-lg border">
                <div className="border-b bg-muted/30 px-4 py-3"><h4 className="text-sm font-medium capitalize">{group}</h4><p className="text-xs text-muted-foreground">{t("related", { group })}</p></div>
                <div className="divide-y">
                {groupPermissions.map((permission) => (
                  <div key={permission.id} className="px-4 py-3">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-medium">{permission.name}</p>
                      {permission.isSystem ? <span className="rounded-full border px-2 py-0.5 text-xs text-muted-foreground">{t("system")}</span> : null}
                    </div>
                    <p className="mt-1 font-mono text-xs text-muted-foreground">{permission.key}</p>
                    {permission.description ? <p className="mt-1 text-xs text-muted-foreground">{permission.description}</p> : null}
                  </div>
                  {(canUpdate || canDelete) && !permission.isSystem ? (
                    <div className="flex gap-1">
                      {canUpdate ? <Button variant="ghost" size="sm" type="button" onClick={() => beginEdit(permission)}>{t("edit")}</Button> : null}
                      {canDelete ? <Button variant="ghost" size="sm" type="button" onClick={() => handleDelete(permission)}>{t("delete")}</Button> : null}
                    </div>
                  ) : null}
                </div>
                  </div>
                ))}
                </div>
              </section>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

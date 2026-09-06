"use client";

import { type FormEvent, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import type { Permission } from "@/features/access/domain/access";
import { accessApi } from "@/features/access/infrastructure/access-api";

type PermissionsCardProps = {
  initialPermissions: Permission[];
  canManage: boolean;
};

export function PermissionsCard({ initialPermissions, canManage }: PermissionsCardProps) {
  const [permissions, setPermissions] = useState(initialPermissions);
  const [editing, setEditing] = useState<Permission | "new">();
  const [key, setKey] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isSaving, setIsSaving] = useState(false);

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
      setPermissions((current) =>
        editing === "new" ? [...current, saved] : current.map((item) => item.id === saved.id ? saved : item),
      );
      cancelEdit();
      toast.success(editing === "new" ? "Permission created." : "Permission updated.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to save this permission.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(permission: Permission) {
    if (!window.confirm(`Delete the “${permission.name}” permission?`)) return;
    try {
      await accessApi.deletePermission(permission.id);
      setPermissions((current) => current.filter((item) => item.id !== permission.id));
      toast.success("Permission deleted.");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to delete this permission.");
    }
  }

  return (
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
        <div>
          <CardTitle>Permissions</CardTitle>
          <CardDescription className="mt-1">System capabilities and custom organization permissions.</CardDescription>
        </div>
        {canManage && !editing ? (
          <Button variant="outline" type="button" onClick={() => beginEdit()}>
            New permission
          </Button>
        ) : null}
      </CardHeader>
      <CardContent>
        {editing ? (
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="permission-name">Name</FieldLabel>
                <Input id="permission-name" value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required />
              </Field>
              <Field>
                <FieldLabel htmlFor="permission-key">Key</FieldLabel>
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
                <FieldLabel htmlFor="permission-description">Description</FieldLabel>
                <Input id="permission-description" value={description} onChange={(event) => setDescription(event.target.value)} maxLength={500} />
              </Field>
              <Field orientation="horizontal" className="justify-end">
                <Button variant="outline" type="button" onClick={cancelEdit} disabled={isSaving}>Cancel</Button>
                <Button type="submit" disabled={isSaving}>{isSaving ? "Saving..." : "Save permission"}</Button>
              </Field>
            </FieldGroup>
          </form>
        ) : (
          <div className="space-y-4">
            {permissions.map((permission, index) => (
              <div key={permission.id}>
                {index > 0 ? <Separator className="mb-4" /> : null}
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-medium">{permission.name}</p>
                      {permission.isSystem ? <span className="rounded-full border px-2 py-0.5 text-xs text-muted-foreground">System</span> : null}
                    </div>
                    <p className="mt-1 font-mono text-xs text-muted-foreground">{permission.key}</p>
                    {permission.description ? <p className="mt-1 text-xs text-muted-foreground">{permission.description}</p> : null}
                  </div>
                  {canManage && !permission.isSystem ? (
                    <div className="flex gap-1">
                      <Button variant="ghost" size="sm" type="button" onClick={() => beginEdit(permission)}>Edit</Button>
                      <Button variant="ghost" size="sm" type="button" onClick={() => handleDelete(permission)}>Delete</Button>
                    </div>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

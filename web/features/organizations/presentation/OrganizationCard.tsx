"use client";

import { type FormEvent, useState } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { platformPermission } from "@/features/access/domain/authorization";
import { usePermission } from "@/features/access/presentation/AuthorizationProvider";
import type { Organization } from "@/features/organizations/domain/organization";
import { organizationApi } from "@/features/organizations/infrastructure/organization-api";
import { useLocalizedFormatters } from "@/features/platform/presentation/useLocalizedFormatters";

type OrganizationCardProps = {
  organization: Organization;
};

export function OrganizationCard({ organization: initialOrganization }: OrganizationCardProps) {
  const t = useTranslations("Organization");
  const canUpdate = usePermission(platformPermission.organizationUpdate);
  const { formatDate } = useLocalizedFormatters();
  const [organization, setOrganization] = useState(initialOrganization);
  const [name, setName] = useState(initialOrganization.name);
  const [slug, setSlug] = useState(initialOrganization.slug);
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [lifecycleAction, setLifecycleAction] = useState<"suspend" | "delete">();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    try {
      const updated = await organizationApi.update({ name, slug });
      setOrganization(updated);
      setName(updated.name);
      setSlug(updated.slug);
      setIsEditing(false);
      toast.success(t("updated"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("updateError"));
    } finally {
      setIsSaving(false);
    }
  }

  function handleCancel() {
    setName(organization.name);
    setSlug(organization.slug);
    setIsEditing(false);
  }

  async function suspendOrganization() {
    if (!window.confirm(t("suspendConfirm"))) return;
    setLifecycleAction("suspend");
    try {
      await organizationApi.suspend();
      toast.success(t("suspended"));
      window.location.replace("/app/settings/organization");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("suspendError"));
      setLifecycleAction(undefined);
    }
  }

  async function deleteOrganization() {
    if (!window.confirm(t("deleteConfirm", { name: organization.name }))) return;
    setLifecycleAction("delete");
    try {
      await organizationApi.delete();
      toast.success(t("deleted"));
      window.location.replace("/onboarding/organization");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("deleteError"));
      setLifecycleAction(undefined);
    }
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
          <div>
            <CardTitle>{t("details")}</CardTitle>
            <CardDescription className="mt-1">{t("detailsDescription")}</CardDescription>
          </div>
          {!isEditing && canUpdate ? (
            <Button variant="outline" type="button" onClick={() => setIsEditing(true)}>
              {t("edit")}
            </Button>
          ) : null}
        </CardHeader>
        <CardContent>
        {isEditing ? (
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="organization-settings-name">{t("name")}</FieldLabel>
                <Input
                  id="organization-settings-name"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  minLength={2}
                  maxLength={160}
                  required
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="organization-settings-slug">{t("workspaceUrl")}</FieldLabel>
                <Input
                  id="organization-settings-slug"
                  value={slug}
                  onChange={(event) => setSlug(event.target.value.toLowerCase())}
                  minLength={2}
                  maxLength={80}
                  pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
                  required
                />
              </Field>
              <Field orientation="horizontal" className="justify-end">
                <Button variant="outline" type="button" onClick={handleCancel} disabled={isSaving}>
                  {t("cancel")}
                </Button>
                <Button type="submit" disabled={isSaving}>
                  {isSaving ? t("saving") : t("save")}
                </Button>
              </Field>
            </FieldGroup>
          </form>
        ) : (
          <dl className="space-y-5">
            <div className="grid gap-1 sm:grid-cols-[140px_1fr] sm:gap-6">
              <dt className="text-sm text-muted-foreground">{t("name")}</dt>
              <dd className="text-sm font-medium">{organization.name}</dd>
            </div>
            <div className="grid gap-1 sm:grid-cols-[140px_1fr] sm:gap-6">
              <dt className="text-sm text-muted-foreground">{t("workspaceUrl")}</dt>
              <dd className="text-sm font-medium">opensources.app/{organization.slug}</dd>
            </div>
            <div className="grid gap-1 sm:grid-cols-[140px_1fr] sm:gap-6">
              <dt className="text-sm text-muted-foreground">{t("yourRole")}</dt>
              <dd className="text-sm font-medium">{t(organization.role)}</dd>
            </div>
          </dl>
        )}
        </CardContent>
      </Card>
      <Card className="border-dashed bg-muted/20">
        <CardHeader>
          <CardTitle>{t("ownership")}</CardTitle>
          <CardDescription>{t("ownershipDescription")}</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">{t("currentAccess", { role: t(organization.role) })}</p>
          <p className="mt-2 text-xs text-muted-foreground">{t("created", { date: formatDate(organization.createdAt) ?? "—" })}</p>
        </CardContent>
      </Card>
      {canUpdate && organization.role === "owner" ? <Card className="border-destructive/40">
        <CardHeader>
          <CardTitle>{t("dangerZone")}</CardTitle>
          <CardDescription>{t("dangerDescription")}</CardDescription>
        </CardHeader>
        <CardContent className="divide-y">
          <div className="flex flex-col gap-4 py-4 first:pt-0 sm:flex-row sm:items-center sm:justify-between">
            <div><p className="text-sm font-medium">{t("suspend")}</p><p className="mt-1 text-sm text-muted-foreground">{t("suspendDescription")}</p></div>
            <Button variant="outline" onClick={suspendOrganization} disabled={lifecycleAction !== undefined}>{t("suspend")}</Button>
          </div>
          <div className="flex flex-col gap-4 py-4 last:pb-0 sm:flex-row sm:items-center sm:justify-between">
            <div><p className="text-sm font-medium">{t("delete")}</p><p className="mt-1 text-sm text-muted-foreground">{t("deleteDescription")}</p></div>
            <Button variant="destructive" onClick={deleteOrganization} disabled={lifecycleAction !== undefined}>{t("delete")}</Button>
          </div>
        </CardContent>
      </Card> : null}
    </div>
  );
}

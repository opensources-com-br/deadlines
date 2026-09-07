"use client";

import { type FormEvent, useState } from "react";
import { toast } from "sonner";
import { useLocale, useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import type { Organization } from "@/features/organizations/domain/organization";
import { organizationApi } from "@/features/organizations/infrastructure/organization-api";

type OrganizationCardProps = {
  organization: Organization;
};

export function OrganizationCard({ organization: initialOrganization }: OrganizationCardProps) {
  const t = useTranslations("Organization");
  const locale = useLocale();
  const [organization, setOrganization] = useState(initialOrganization);
  const [name, setName] = useState(initialOrganization.name);
  const [slug, setSlug] = useState(initialOrganization.slug);
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

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

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
          <div>
            <CardTitle>{t("details")}</CardTitle>
            <CardDescription className="mt-1">{t("detailsDescription")}</CardDescription>
          </div>
          {!isEditing && organization.role === "owner" ? (
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
              <dd className="text-sm font-medium">deadlines.app/{organization.slug}</dd>
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
          <p className="mt-2 text-xs text-muted-foreground">{t("created", { date: new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(new Date(organization.createdAt)) })}</p>
        </CardContent>
      </Card>
    </div>
  );
}

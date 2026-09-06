"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { organizationApi } from "@/features/organizations/infrastructure/organization-api";

function toSlug(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 80);
}

export function CreateOrganizationScreen() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugWasEdited, setSlugWasEdited] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSigningOut, setIsSigningOut] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await organizationApi.create({ name, slug });
      toast.success("Your organization is ready.");
      router.replace("/app");
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to create your organization.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSignOut() {
    setIsSigningOut(true);
    await fetch("/api/auth/logout", { method: "POST" });
    router.replace("/login");
    router.refresh();
  }

  return (
    <main className="relative flex min-h-svh items-center justify-center bg-muted/30 p-6 text-foreground md:p-10">
      <div className="absolute left-6 top-6 flex items-center gap-2 text-lg font-semibold md:left-10 md:top-10">
        <Image className="size-8 invert" src="/deadlines-mark.png" alt="" width={32} height={32} priority />
        Deadlines
      </div>
      <Button
        className="absolute right-6 top-6 md:right-10 md:top-10"
        variant="ghost"
        type="button"
        onClick={handleSignOut}
        disabled={isSigningOut}
      >
        {isSigningOut ? "Signing out..." : "Log out"}
      </Button>

      <Card className="w-full max-w-sm py-8">
        <CardHeader className="px-8">
          <CardTitle>Create your organization</CardTitle>
          <CardDescription>This will be the workspace for you and your team.</CardDescription>
        </CardHeader>
        <CardContent className="px-8">
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="organization-name">Organization name</FieldLabel>
                <Input
                  id="organization-name"
                  value={name}
                  onChange={(event) => {
                    const nextName = event.target.value;
                    setName(nextName);
                    if (!slugWasEdited) setSlug(toSlug(nextName));
                  }}
                  minLength={2}
                  maxLength={160}
                  autoComplete="organization"
                  placeholder="Acme Inc"
                  required
                  autoFocus
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="organization-slug">Workspace URL</FieldLabel>
                <Input
                  id="organization-slug"
                  value={slug}
                  onChange={(event) => {
                    setSlugWasEdited(true);
                    setSlug(toSlug(event.target.value));
                  }}
                  minLength={2}
                  maxLength={80}
                  pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
                  placeholder="acme-inc"
                  required
                />
                <FieldDescription>deadlines.app/{slug || "your-workspace"}</FieldDescription>
              </Field>
              <Field>
                <Button className="w-full" type="submit" disabled={isSubmitting}>
                  {isSubmitting ? "Creating..." : "Create organization"}
                </Button>
              </Field>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}

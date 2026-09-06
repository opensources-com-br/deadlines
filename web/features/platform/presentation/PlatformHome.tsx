"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { updateUserProfile } from "@/features/platform/infrastructure/profile-api";

type PlatformHomeProps = {
  user: UserProfile;
};

export function PlatformHome({ user }: PlatformHomeProps) {
  const router = useRouter();
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [firstName, setFirstName] = useState(user.profile.firstName);
  const [lastName, setLastName] = useState(user.profile.lastName);

  async function handleSignOut() {
    setIsSigningOut(true);
    await fetch("/api/auth/logout", { method: "POST" });
    toast.success("You have been signed out.");
    router.replace("/login");
    router.refresh();
  }

  async function handleProfileUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);

    try {
      const updated = await updateUserProfile({ firstName, lastName });
      setFirstName(updated.profile.firstName);
      setLastName(updated.profile.lastName);
      toast.success("Your profile has been updated.");
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to update your profile.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <main className="min-h-svh bg-background text-foreground">
      <header className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-5">
        <div className="flex items-center gap-2 text-lg font-semibold tracking-tight">
          <Image className="size-8 invert" src="/deadlines-mark.png" alt="" width={32} height={32} priority />
          Deadlines
        </div>
        <Button variant="outline" type="button" onClick={handleSignOut} disabled={isSigningOut}>
          {isSigningOut ? "Signing out..." : "Log out"}
        </Button>
      </header>

      <section className="mx-auto grid w-full max-w-6xl gap-10 px-6 py-16 lg:grid-cols-[minmax(0,0.8fr)_minmax(420px,1fr)] lg:py-20">
        <div className="max-w-xl">
          <p className="text-sm font-medium text-muted-foreground">Platform</p>
          <h1 className="mt-4 text-4xl font-semibold tracking-tight sm:text-5xl">
            Welcome, {firstName}.
          </h1>
          <p className="mt-5 text-lg leading-8 text-muted-foreground">
            Your account is active. You can review and update your profile here.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Your profile</CardTitle>
            <CardDescription>Update the information associated with your account.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleProfileUpdate}>
              <FieldGroup>
                <Field>
                  <FieldLabel htmlFor="profile-email">Email</FieldLabel>
                  <Input id="profile-email" type="email" value={user.email} disabled />
                </Field>
                <div className="grid gap-6 sm:grid-cols-2">
                  <Field>
                    <FieldLabel htmlFor="profile-first-name">First name</FieldLabel>
                    <Input
                      id="profile-first-name"
                      value={firstName}
                      onChange={(event) => setFirstName(event.target.value)}
                      maxLength={100}
                      required
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="profile-last-name">Last name</FieldLabel>
                    <Input
                      id="profile-last-name"
                      value={lastName}
                      onChange={(event) => setLastName(event.target.value)}
                      maxLength={100}
                      required
                    />
                  </Field>
                </div>
                <Field orientation="horizontal" className="justify-end">
                  <Button type="submit" disabled={isSaving}>
                    {isSaving ? "Saving..." : "Save changes"}
                  </Button>
                </Field>
              </FieldGroup>
            </form>
          </CardContent>
        </Card>
      </section>
    </main>
  );
}

"use client";

import { type FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { changePassword, updateUserProfile } from "@/features/platform/infrastructure/profile-api";

export function AccountSettings({ user }: { user: UserProfile }) {
  const router = useRouter();
  const [isEditing, setIsEditing] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [firstName, setFirstName] = useState(user.profile.firstName);
  const [lastName, setLastName] = useState(user.profile.lastName);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [locale, setLocale] = useState("pt-BR");

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setIsSaving(true);
    try { const updated = await updateUserProfile({ firstName, lastName }); setFirstName(updated.profile.firstName); setLastName(updated.profile.lastName); setIsEditing(false); router.refresh(); toast.success("Your profile has been updated."); }
    catch (error) { toast.error(error instanceof Error ? error.message : "Unable to update your profile."); }
    finally { setIsSaving(false); }
  }
  async function savePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (newPassword !== passwordConfirmation) { toast.error("Passwords do not match."); return; }
    setIsSaving(true);
    try { await changePassword(currentPassword, newPassword); setCurrentPassword(""); setNewPassword(""); setPasswordConfirmation(""); setIsChangingPassword(false); toast.success("Your password has been changed."); }
    catch (error) { toast.error(error instanceof Error ? error.message : "Unable to change your password."); }
    finally { setIsSaving(false); }
  }

  return <div className="space-y-6">
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4"><div><CardTitle>Profile</CardTitle><CardDescription>Your personal information used across Deadlines.</CardDescription></div>{!isEditing ? <Button variant="outline" onClick={() => setIsEditing(true)}>Edit profile</Button> : null}</CardHeader>
      <CardContent>{isEditing ? <form onSubmit={saveProfile}><FieldGroup><div className="grid gap-6 sm:grid-cols-2"><Field><FieldLabel htmlFor="profile-first-name">First name</FieldLabel><Input id="profile-first-name" value={firstName} onChange={(event) => setFirstName(event.target.value)} maxLength={100} required /></Field><Field><FieldLabel htmlFor="profile-last-name">Last name</FieldLabel><Input id="profile-last-name" value={lastName} onChange={(event) => setLastName(event.target.value)} maxLength={100} required /></Field></div><Field><FieldLabel htmlFor="profile-email">Email</FieldLabel><Input id="profile-email" type="email" value={user.email} disabled /><p className="text-xs text-muted-foreground">Your email address cannot be changed here.</p></Field><Field orientation="horizontal" className="justify-end"><Button variant="outline" type="button" disabled={isSaving} onClick={() => { setFirstName(user.profile.firstName); setLastName(user.profile.lastName); setIsEditing(false); }}>Cancel</Button><Button type="submit" disabled={isSaving}>{isSaving ? "Saving..." : "Save changes"}</Button></Field></FieldGroup></form> : <dl className="grid gap-5 sm:grid-cols-2"><div><dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Name</dt><dd className="mt-1 font-medium">{firstName} {lastName}</dd></div><div><dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Email</dt><dd className="mt-1 font-medium">{user.email}</dd></div></dl>}</CardContent>
    </Card>
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4"><div><CardTitle>Password</CardTitle><CardDescription>Choose a strong password to protect your account.</CardDescription></div>{!isChangingPassword ? <Button variant="outline" onClick={() => setIsChangingPassword(true)}>Change password</Button> : null}</CardHeader>
      {isChangingPassword ? <CardContent><form onSubmit={savePassword}><FieldGroup><Field><FieldLabel htmlFor="current-password">Current password</FieldLabel><Input id="current-password" type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required /></Field><Field><FieldLabel htmlFor="new-password">New password</FieldLabel><Input id="new-password" type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength={12} maxLength={72} required /><p className="text-xs text-muted-foreground">Use between 12 and 72 characters.</p></Field><Field><FieldLabel htmlFor="password-confirmation">Confirm new password</FieldLabel><Input id="password-confirmation" type="password" autoComplete="new-password" value={passwordConfirmation} onChange={(event) => setPasswordConfirmation(event.target.value)} minLength={12} maxLength={72} required /></Field><Field orientation="horizontal" className="justify-end"><Button variant="outline" type="button" disabled={isSaving} onClick={() => { setCurrentPassword(""); setNewPassword(""); setPasswordConfirmation(""); setIsChangingPassword(false); }}>Cancel</Button><Button type="submit" disabled={isSaving}>{isSaving ? "Changing..." : "Update password"}</Button></Field></FieldGroup></form></CardContent> : null}
    </Card>
    <Card>
      <CardHeader>
        <CardTitle>Language and region</CardTitle>
        <CardDescription>Choose the language used for navigation, dates, and messages.</CardDescription>
      </CardHeader>
      <CardContent>
        <Field className="max-w-sm">
          <FieldLabel>Language</FieldLabel>
          <Select value={locale} onValueChange={(value) => value && setLocale(value)}>
            <SelectTrigger className="w-full" aria-label="Language">
              <SelectValue />
            </SelectTrigger>
            <SelectContent align="start">
              <SelectItem value="pt-BR">Português (Brasil)</SelectItem>
              <SelectItem value="en">English</SelectItem>
            </SelectContent>
          </Select>
          <p className="text-xs text-muted-foreground">Dates and numbers are displayed using this language.</p>
        </Field>
      </CardContent>
    </Card>
  </div>;
}

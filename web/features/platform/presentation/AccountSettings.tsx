"use client";

import { type FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { UserProfile } from "@/features/platform/domain/user-profile";
import { changePassword, requestEmailChange, updateUserProfile } from "@/features/platform/infrastructure/profile-api";
import { updatePreferences } from "@/features/platform/infrastructure/preference-api";
import { useUserPreferences } from "@/features/platform/presentation/UserPreferenceProvider";
import { AccountDangerZone } from "@/features/platform/presentation/AccountDangerZone";

const timezones = typeof Intl.supportedValuesOf === "function"
  ? Intl.supportedValuesOf("timeZone")
  : ["UTC", "America/Sao_Paulo", "America/New_York", "Europe/London", "Asia/Tokyo"];

export function AccountSettings({ user }: { user: UserProfile }) {
  const router = useRouter();
  const locale = useLocale();
  const tLocale = useTranslations("LocaleSwitcher");
  const t = useTranslations("Account");
  const { preferences, setPreferences } = useUserPreferences();
  const [isEditing, setIsEditing] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [isChangingEmail, setIsChangingEmail] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [firstName, setFirstName] = useState(user.profile.firstName);
  const [lastName, setLastName] = useState(user.profile.lastName);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [email, setEmail] = useState(user.email);
  const [emailPassword, setEmailPassword] = useState("");
  const [isChangingLocale, setIsChangingLocale] = useState(false);

  async function changeLocale(nextLocale: string) {
    if (nextLocale === locale) return;
    setIsChangingLocale(true);
    try {
      const response = await fetch("/api/locale", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ locale: nextLocale }),
      });
      if (!response.ok) throw new Error(tLocale("error"));
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : tLocale("error"));
    } finally {
      setIsChangingLocale(false);
    }
  }
  async function changePreference(input: { timezone?: string; theme?: "light" | "dark" | "system" }) {
    setIsChangingLocale(true);
    try {
      const updated = await updatePreferences(input);
      setPreferences(updated);
      toast.success(tLocale("preferenceSaved"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : tLocale("preferenceError"));
    } finally {
      setIsChangingLocale(false);
    }
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setIsSaving(true);
    try { const updated = await updateUserProfile({ firstName, lastName }); setFirstName(updated.profile.firstName); setLastName(updated.profile.lastName); setIsEditing(false); router.refresh(); toast.success(t("profileUpdated")); }
    catch (error) { toast.error(error instanceof Error ? error.message : t("profileError")); }
    finally { setIsSaving(false); }
  }
  async function savePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (newPassword !== passwordConfirmation) { toast.error(t("passwordMismatch")); return; }
    setIsSaving(true);
    try { await changePassword(currentPassword, newPassword); setCurrentPassword(""); setNewPassword(""); setPasswordConfirmation(""); setIsChangingPassword(false); toast.success(t("passwordUpdated")); }
    catch (error) { toast.error(error instanceof Error ? error.message : t("passwordError")); }
    finally { setIsSaving(false); }
  }
  async function saveEmail(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setIsSaving(true);
    try { await requestEmailChange(email, emailPassword); setEmailPassword(""); setIsChangingEmail(false); toast.success(t("emailChangeRequested")); }
    catch (error) { toast.error(error instanceof Error ? error.message : t("emailChangeError")); }
    finally { setIsSaving(false); }
  }

  return <div className="space-y-6">
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4"><div><CardTitle>{t("profile")}</CardTitle><CardDescription>{t("profileDescription")}</CardDescription></div>{!isEditing ? <Button variant="outline" onClick={() => setIsEditing(true)}>{t("editProfile")}</Button> : null}</CardHeader>
      <CardContent>{isEditing ? <form onSubmit={saveProfile}><FieldGroup><div className="grid gap-6 sm:grid-cols-2"><Field><FieldLabel htmlFor="profile-first-name">{t("firstName")}</FieldLabel><Input id="profile-first-name" value={firstName} onChange={(event) => setFirstName(event.target.value)} maxLength={100} required /></Field><Field><FieldLabel htmlFor="profile-last-name">{t("lastName")}</FieldLabel><Input id="profile-last-name" value={lastName} onChange={(event) => setLastName(event.target.value)} maxLength={100} required /></Field></div><Field><FieldLabel htmlFor="profile-email">{t("email")}</FieldLabel><Input id="profile-email" type="email" value={user.email} disabled /><p className="text-xs text-muted-foreground">{t("emailHelp")}</p></Field><Field orientation="horizontal" className="justify-end"><Button variant="outline" type="button" disabled={isSaving} onClick={() => { setFirstName(user.profile.firstName); setLastName(user.profile.lastName); setIsEditing(false); }}>{t("cancel")}</Button><Button type="submit" disabled={isSaving}>{isSaving ? t("saving") : t("save")}</Button></Field></FieldGroup></form> : <dl className="grid gap-5 sm:grid-cols-2"><div><dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{t("name")}</dt><dd className="mt-1 font-medium">{firstName} {lastName}</dd></div><div><dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{t("email")}</dt><dd className="mt-1 font-medium">{user.email}</dd></div></dl>}</CardContent>
    </Card>
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4"><div><CardTitle>{t("emailChange")}</CardTitle><CardDescription>{t("emailChangeDescription")}</CardDescription></div>{!isChangingEmail ? <Button variant="outline" onClick={() => setIsChangingEmail(true)}>{t("changeEmail")}</Button> : null}</CardHeader>
      {isChangingEmail ? <CardContent><form onSubmit={saveEmail}><FieldGroup><Field><FieldLabel htmlFor="new-email">{t("newEmail")}</FieldLabel><Input id="new-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required /></Field><Field><FieldLabel htmlFor="email-password">{t("accountPassword")}</FieldLabel><Input id="email-password" type="password" value={emailPassword} onChange={(event) => setEmailPassword(event.target.value)} autoComplete="current-password" required /></Field><Field orientation="horizontal" className="justify-end"><Button variant="outline" type="button" disabled={isSaving} onClick={() => { setEmail(user.email); setEmailPassword(""); setIsChangingEmail(false); }}>{t("cancel")}</Button><Button type="submit" disabled={isSaving}>{isSaving ? t("saving") : t("requestEmailChange")}</Button></Field></FieldGroup></form></CardContent> : null}
    </Card>
    <Card>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4"><div><CardTitle>{t("password")}</CardTitle><CardDescription>{t("passwordDescription")}</CardDescription></div>{!isChangingPassword ? <Button variant="outline" onClick={() => setIsChangingPassword(true)}>{t("changePassword")}</Button> : null}</CardHeader>
      {isChangingPassword ? <CardContent><form onSubmit={savePassword}><FieldGroup><Field><FieldLabel htmlFor="current-password">{t("currentPassword")}</FieldLabel><Input id="current-password" type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required /></Field><Field><FieldLabel htmlFor="new-password">{t("newPassword")}</FieldLabel><Input id="new-password" type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength={12} maxLength={72} required /><p className="text-xs text-muted-foreground">{t("passwordHelp")}</p></Field><Field><FieldLabel htmlFor="password-confirmation">{t("confirmPassword")}</FieldLabel><Input id="password-confirmation" type="password" autoComplete="new-password" value={passwordConfirmation} onChange={(event) => setPasswordConfirmation(event.target.value)} minLength={12} maxLength={72} required /></Field><Field orientation="horizontal" className="justify-end"><Button variant="outline" type="button" disabled={isSaving} onClick={() => { setCurrentPassword(""); setNewPassword(""); setPasswordConfirmation(""); setIsChangingPassword(false); }}>{t("cancel")}</Button><Button type="submit" disabled={isSaving}>{isSaving ? t("changing") : t("updatePassword")}</Button></Field></FieldGroup></form></CardContent> : null}
    </Card>
    <Card>
      <CardHeader>
        <CardTitle>{tLocale("title")}</CardTitle><CardDescription>{tLocale("description")}</CardDescription>
      </CardHeader>
      <CardContent>
        <Field className="max-w-sm">
          <FieldLabel>{tLocale("label")}</FieldLabel>
          <Select value={locale} disabled={isChangingLocale} onValueChange={(value) => value && void changeLocale(value)}>
            <SelectTrigger className="w-full" aria-label={tLocale("label")}>
              <SelectValue />
            </SelectTrigger>
            <SelectContent align="start">
              <SelectItem value="pt-BR">Português (Brasil)</SelectItem>
              <SelectItem value="en">English</SelectItem>
            </SelectContent>
          </Select>
          <p className="text-xs text-muted-foreground">{tLocale("help")}</p>
        </Field>
        <div className="mt-6 grid gap-6 sm:grid-cols-2">
          <Field>
            <FieldLabel>{tLocale("timezone")}</FieldLabel>
            <Select value={preferences.timezone} disabled={isChangingLocale} onValueChange={(value) => value && void changePreference({ timezone: value })}>
              <SelectTrigger className="w-full" aria-label={tLocale("timezone")}><SelectValue /></SelectTrigger>
              <SelectContent align="start" className="max-h-80">
                {!timezones.includes(preferences.timezone) ? <SelectItem value={preferences.timezone}>{preferences.timezone}</SelectItem> : null}
                {timezones.map((timezone) => <SelectItem key={timezone} value={timezone}>{timezone.replaceAll("_", " ")}</SelectItem>)}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">{tLocale("timezoneHelp")}</p>
          </Field>
          <Field>
            <FieldLabel>{tLocale("theme")}</FieldLabel>
            <Select value={preferences.theme} disabled={isChangingLocale} onValueChange={(value) => value && void changePreference({ theme: value as "light" | "dark" | "system" })}>
              <SelectTrigger className="w-full" aria-label={tLocale("theme")}><SelectValue /></SelectTrigger>
              <SelectContent align="start">
                <SelectItem value="system">{tLocale("system")}</SelectItem><SelectItem value="light">{tLocale("light")}</SelectItem><SelectItem value="dark">{tLocale("dark")}</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">{tLocale("themeHelp")}</p>
          </Field>
        </div>
      </CardContent>
    </Card>
    <AccountDangerZone />
  </div>;
}

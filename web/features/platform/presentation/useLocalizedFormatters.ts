"use client";

import { useLocale } from "next-intl";

import { useUserPreferences } from "@/features/platform/presentation/UserPreferenceProvider";

export function useLocalizedFormatters() {
  const locale = useLocale();
  const { preferences } = useUserPreferences();

  function formatDate(value: string | Date, options: Intl.DateTimeFormatOptions = { dateStyle: "medium" }) {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return undefined;
    return new Intl.DateTimeFormat(locale, { ...options, timeZone: preferences.timezone }).format(date);
  }

  function formatCurrency(value: number, currency = "USD") {
    return new Intl.NumberFormat(locale, { style: "currency", currency }).format(value);
  }

  return { locale, timezone: preferences.timezone, formatDate, formatCurrency };
}
